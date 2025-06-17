package com.jdc.tools.precommit.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import com.jdc.tools.precommit.util.FileTypeFilter;
import git4idea.GitUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Git分析服务
 * 负责检测Git暂存区文件、分析文件变动内容
 */
@Service
public final class GitAnalysisService {

    private static final Logger LOG = Logger.getInstance(GitAnalysisService.class);

    // 文件大小限制 (1MB)
    private static final long MAX_FILE_SIZE = 1024 * 1024;

    // 注意：文件类型过滤现在由FileTypeFilter类处理

    public static GitAnalysisService getInstance() {
        return ApplicationManager.getApplication().getService(GitAnalysisService.class);
    }

    /**
     * 获取暂存区文件信息
     */
    public List<StagedFileInfo> getStagedFiles(Project project) {
        List<StagedFileInfo> stagedFiles = new ArrayList<>();

        try {
            // 获取Git仓库
            GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
            Collection<GitRepository> repositories = repositoryManager.getRepositories();

            if (repositories.isEmpty()) {
                LOG.warn("项目中没有找到Git仓库");
                return stagedFiles;
            }

            // 获取变更列表管理器
            ChangeListManager changeListManager = ChangeListManager.getInstance(project);

            // 获取默认变更列表（通常包含暂存的文件）
            LocalChangeList defaultChangeList = changeListManager.getDefaultChangeList();
            Collection<Change> changes = defaultChangeList.getChanges();

            for (Change change : changes) {
                try {
                    StagedFileInfo fileInfo = analyzeChange(change, project);
                    if (fileInfo != null && isValidCodeFile(fileInfo)) {
                        stagedFiles.add(fileInfo);
                    }
                } catch (Exception e) {
                    LOG.error("分析文件变更失败", e);
                }
            }

            LOG.info("检测到 " + stagedFiles.size() + " 个暂存文件");

        } catch (Exception e) {
            LOG.error("获取暂存区文件失败", e);
        }

        return stagedFiles;
    }

    /**
     * 分析单个文件变更
     */
    private StagedFileInfo analyzeChange(Change change, Project project) {
        try {
            VirtualFile virtualFile = null;
            String filePath = null;
            String fileName = null;

            // 获取文件信息
            if (change.getAfterRevision() != null) {
                // 修改或新增的文件
                virtualFile = change.getAfterRevision().getFile().getVirtualFile();
                filePath = change.getAfterRevision().getFile().getPath();
                fileName = change.getAfterRevision().getFile().getName();
            } else if (change.getBeforeRevision() != null) {
                // 删除的文件
                filePath = change.getBeforeRevision().getFile().getPath();
                fileName = change.getBeforeRevision().getFile().getName();
            }

            if (filePath == null) {
                return null;
            }

            // 获取文件内容
            String currentContent = "";
            boolean isFileTooLarge = false;
            boolean isUnsupportedFile = false;

            if (virtualFile != null && virtualFile.exists()) {
                try {
                    // 检查文件大小
                    long fileSize = virtualFile.getLength();
                    if (fileSize > MAX_FILE_SIZE) {
                        isFileTooLarge = true;
                        currentContent = String.format("文件过大 (%d bytes > %d bytes)，跳过内容分析",
                            fileSize, MAX_FILE_SIZE);
                        LOG.warn("文件过大，跳过分析: " + filePath + " (" + fileSize + " bytes)");
                    } else {
                        // 使用FileTypeFilter检查文件类型
                        if (!FileTypeFilter.isCodeFile(virtualFile)) {
                            isUnsupportedFile = true;
                            String extension = getFileExtension(fileName);
                            currentContent = "不支持的文件类型: " + extension;
                            LOG.info("跳过不支持的文件类型: " + filePath + " (." + extension + ")");
                        } else {
                            currentContent = new String(virtualFile.contentsToByteArray(), StandardCharsets.UTF_8);
                        }
                    }
                } catch (IOException e) {
                    LOG.warn("读取文件内容失败: " + filePath, e);
                    currentContent = "读取文件内容失败: " + e.getMessage();
                }
            }

            // 获取变更类型
            Change.Type changeType = change.getType();
            String changeTypeStr = getChangeTypeString(changeType);

            // 获取差异信息
            String diffContent = getDiffContent(change, project);

            return new StagedFileInfo(
                fileName,
                filePath,
                currentContent,
                changeTypeStr,
                diffContent,
                detectLanguage(fileName)
            );

        } catch (Exception e) {
            LOG.error("分析文件变更失败", e);
            return null;
        }
    }

    /**
     * 获取文件差异内容
     */
    private String getDiffContent(Change change, Project project) {
        try {
            StringBuilder diff = new StringBuilder();

            // 获取Git仓库
            GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
            Collection<GitRepository> repositories = repositoryManager.getRepositories();

            if (repositories.isEmpty()) {
                return getSimpleDiffContent(change);
            }

            GitRepository repository = repositories.iterator().next();
            String filePath = null;

            if (change.getAfterRevision() != null) {
                filePath = change.getAfterRevision().getFile().getPath();
            } else if (change.getBeforeRevision() != null) {
                filePath = change.getBeforeRevision().getFile().getPath();
            }

            if (filePath == null) {
                return "无法确定文件路径";
            }

            // 获取相对路径
            String relativePath = getRelativePath(repository.getRoot().getPath(), filePath);

            try {
                // 使用Git命令获取暂存区差异
                GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.DIFF);
                handler.addParameters("--cached"); // 获取暂存区差异
                handler.addParameters("--no-color"); // 不使用颜色
                handler.addParameters("--unified=3"); // 显示3行上下文
                handler.addParameters(relativePath);

                String gitDiffOutput = Git.getInstance().runCommand(handler).getOutputOrThrow();

                if (!gitDiffOutput.trim().isEmpty()) {
                    diff.append("=== Git差异信息 ===\n");
                    diff.append(gitDiffOutput);
                } else {
                    // 如果没有暂存区差异，尝试获取工作区差异
                    handler = new GitLineHandler(project, repository.getRoot(), GitCommand.DIFF);
                    handler.addParameters("--no-color");
                    handler.addParameters("--unified=3");
                    handler.addParameters(relativePath);

                    gitDiffOutput = Git.getInstance().runCommand(handler).getOutputOrThrow();

                    if (!gitDiffOutput.trim().isEmpty()) {
                        diff.append("=== 工作区差异信息 ===\n");
                        diff.append(gitDiffOutput);
                    } else {
                        diff.append(getSimpleDiffContent(change));
                    }
                }

            } catch (VcsException e) {
                LOG.warn("Git diff命令执行失败: " + e.getMessage());
                diff.append(getSimpleDiffContent(change));
                diff.append("\n注意: 无法获取详细Git差异信息 - ").append(e.getMessage());
            }

            return diff.toString();

        } catch (Exception e) {
            LOG.error("获取差异内容失败", e);
            return getSimpleDiffContent(change) + "\n错误: " + e.getMessage();
        }
    }

    /**
     * 获取简单的差异内容（备用方案）
     */
    private String getSimpleDiffContent(Change change) {
        StringBuilder diff = new StringBuilder();

        if (change.getBeforeRevision() != null && change.getAfterRevision() != null) {
            // 文件修改
            diff.append("=== 文件修改 ===\n");
            diff.append("文件: ").append(change.getAfterRevision().getFile().getPath()).append("\n");
            diff.append("变更类型: 修改\n");
        } else if (change.getBeforeRevision() == null) {
            // 新增文件
            diff.append("=== 新增文件 ===\n");
            diff.append("文件: ").append(change.getAfterRevision().getFile().getPath()).append("\n");
            diff.append("变更类型: 新增\n");
        } else if (change.getAfterRevision() == null) {
            // 删除文件
            diff.append("=== 删除文件 ===\n");
            diff.append("文件: ").append(change.getBeforeRevision().getFile().getPath()).append("\n");
            diff.append("变更类型: 删除\n");
        }

        return diff.toString();
    }

    /**
     * 获取相对路径
     */
    private String getRelativePath(String basePath, String fullPath) {
        if (fullPath.startsWith(basePath)) {
            String relativePath = fullPath.substring(basePath.length());
            if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
                relativePath = relativePath.substring(1);
            }
            return relativePath;
        }
        return fullPath;
    }

    /**
     * 获取变更类型字符串
     */
    private String getChangeTypeString(Change.Type changeType) {
        switch (changeType) {
            case NEW:
                return "新增";
            case DELETED:
                return "删除";
            case MOVED:
                return "移动";
            case MODIFICATION:
                return "修改";
            default:
                return "未知";
        }
    }

    /**
     * 检测编程语言
     */
    private String detectLanguage(String fileName) {
        if (fileName == null) {
            return "unknown";
        }

        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }

        switch (extension) {
            case "java":
                return "java";
            case "kt":
                return "kotlin";
            case "py":
                return "python";
            case "js":
                return "javascript";
            case "ts":
                return "typescript";
            case "go":
                return "go";
            case "rs":
                return "rust";
            case "cpp":
            case "cc":
            case "cxx":
                return "cpp";
            case "c":
                return "c";
            case "cs":
                return "csharp";
            case "php":
                return "php";
            case "rb":
                return "ruby";
            case "scala":
                return "scala";
            case "swift":
                return "swift";
            case "sql":
                return "sql";
            case "xml":
                return "xml";
            case "json":
                return "json";
            case "yaml":
            case "yml":
                return "yaml";
            case "md":
                return "markdown";
            default:
                return "text";
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 检查文件是否为有效的代码文件
     */
    private boolean isValidCodeFile(StagedFileInfo fileInfo) {
        if (fileInfo == null || fileInfo.getFileName() == null) {
            return false;
        }

        // 检查文件内容是否表明文件不可用
        String content = fileInfo.getContent();
        if (content.startsWith("文件过大") || content.startsWith("不支持的文件类型") || content.startsWith("读取文件内容失败")) {
            LOG.info("跳过无效文件: " + fileInfo.getFileName() + " - " + content.substring(0, Math.min(50, content.length())));
            return false;
        }

        return true;
    }

    /**
     * 检查文件是否应该被审查（基于文件名）
     */
    public boolean shouldReviewFile(String fileName, long fileSize) {
        if (fileName == null) {
            return false;
        }

        // 检查文件大小
        if (fileSize > MAX_FILE_SIZE) {
            return false;
        }

        // 基于文件名进行简单的文件类型检查
        return isCodeFileByName(fileName);
    }

    /**
     * 基于文件名判断是否为代码文件
     */
    private boolean isCodeFileByName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        String lowerFileName = fileName.toLowerCase();
        String extension = getFileExtension(fileName);

        // 检查是否在排除列表中
        if (lowerFileName.contains(".lock") || lowerFileName.contains(".bin") ||
            lowerFileName.contains(".cache") || lowerFileName.contains(".tmp")) {
            return false;
        }

        // 检查扩展名
        if (extension != null && !extension.isEmpty()) {
            // 使用简化的扩展名检查逻辑
            return isCodeExtension(extension);
        }

        // 检查无扩展名的常见代码文件
        return lowerFileName.equals("makefile") || lowerFileName.equals("dockerfile") ||
               lowerFileName.equals("rakefile") || lowerFileName.equals("gemfile");
    }

    /**
     * 检查扩展名是否为代码文件扩展名
     */
    private boolean isCodeExtension(String extension) {
        String ext = extension.toLowerCase();

        // 常见的代码文件扩展名
        return ext.matches("java|kt|py|js|jsx|ts|tsx|vue|html|css|scss|sass|less|" +
                          "xml|json|yaml|yml|sql|go|rs|php|rb|cs|cpp|c|h|hpp|" +
                          "swift|scala|groovy|clj|sh|bash|ps1|md|txt|conf|properties");
    }

    /**
     * 获取支持的文件统计信息
     */
    public Map<String, Object> getFileStats(List<StagedFileInfo> files) {
        Map<String, Object> stats = new HashMap<>();

        int totalFiles = files.size();
        int supportedFiles = 0;
        int oversizedFiles = 0;
        long totalSize = 0;

        Map<String, Integer> languageCount = new HashMap<>();

        for (StagedFileInfo file : files) {
            totalSize += file.getContent().length();

            if (file.getContent().startsWith("文件过大")) {
                oversizedFiles++;
            } else if (!file.getContent().startsWith("不支持的文件类型")) {
                supportedFiles++;
                String language = file.getLanguage();
                languageCount.put(language, languageCount.getOrDefault(language, 0) + 1);
            }
        }

        stats.put("totalFiles", totalFiles);
        stats.put("supportedFiles", supportedFiles);
        stats.put("oversizedFiles", oversizedFiles);
        stats.put("totalSize", totalSize);
        stats.put("languageDistribution", languageCount);

        return stats;
    }

    /**
     * 检查项目是否为Git仓库
     */
    public boolean isGitRepository(Project project) {
        try {
            GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
            return !repositoryManager.getRepositories().isEmpty();
        } catch (Exception e) {
            LOG.error("检查Git仓库状态失败", e);
            return false;
        }
    }

    /**
     * 暂存文件信息类
     */
    public static class StagedFileInfo {
        private final String fileName;
        private final String filePath;
        private final String content;
        private final String changeType;
        private final String diffContent;
        private final String language;

        public StagedFileInfo(String fileName, String filePath, String content,
                             String changeType, String diffContent, String language) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.content = content;
            this.changeType = changeType;
            this.diffContent = diffContent;
            this.language = language;
        }

        // Getters
        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public String getContent() { return content; }
        public String getChangeType() { return changeType; }
        public String getDiffContent() { return diffContent; }
        public String getLanguage() { return language; }

        @Override
        public String toString() {
            return String.format("StagedFileInfo{fileName='%s', changeType='%s', language='%s'}",
                fileName, changeType, language);
        }
    }
}
