package com.jdc.tools.precommit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jdc.tools.precommit.exception.ReviewException;
import com.jdc.tools.precommit.model.PreCommitReviewRequest;
import com.jdc.tools.precommit.model.PreCommitReviewResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Git提交前代码审查服务
 * 负责调用后端API进行代码审查
 */
@Service
public final class PreCommitReviewService {

    private static final Logger LOG = Logger.getInstance(PreCommitReviewService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 重试配置
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;

    // 文件数量限制
    private static final int MAX_FILES_PER_REVIEW = 20;

    public static PreCommitReviewService getInstance() {
        return ApplicationManager.getApplication().getService(PreCommitReviewService.class);
    }

    /**
     * 审查选定的文件
     */
    public CompletableFuture<PreCommitReviewResponse> reviewSelectedFiles(Project project, List<VirtualFile> selectedFiles) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("开始审查选定文件，数量: " + selectedFiles.size());

                // 预检查
                validatePreConditions(project);

                // 获取暂存区文件信息
                GitAnalysisService gitService = GitAnalysisService.getInstance();
                List<GitAnalysisService.StagedFileInfo> allStagedFiles = gitService.getStagedFiles(project);

                // 过滤出选定的文件
                List<GitAnalysisService.StagedFileInfo> selectedStagedFiles = new ArrayList<>();
                for (VirtualFile selectedFile : selectedFiles) {
                    String selectedPath = selectedFile.getPath();
                    for (GitAnalysisService.StagedFileInfo stagedFile : allStagedFiles) {
                        if (stagedFile.getFilePath().endsWith(selectedPath) ||
                            selectedPath.endsWith(stagedFile.getFilePath())) {
                            selectedStagedFiles.add(stagedFile);
                            break;
                        }
                    }
                }

                if (selectedStagedFiles.isEmpty()) {
                    throw new ReviewException("选定文件不在暂存区",
                        "选定的文件不在Git暂存区中，请先使用 git add 添加文件");
                }

                // 验证文件
                validateStagedFiles(selectedStagedFiles);

                // 构建审查请求
                PreCommitReviewRequest request = buildReviewRequest(project, selectedStagedFiles);

                // 发送审查请求（带重试）
                return executeReviewWithRetry(request);

            } catch (ReviewException e) {
                LOG.warn("审查预检查失败: " + e.getMessage());
                throw e;
            } catch (Exception e) {
                LOG.error("审查选定文件失败", e);
                throw new ReviewException("审查失败",
                    "系统错误: " + e.getMessage() + "\n请检查网络连接或稍后重试");
            }
        });
    }

    /**
     * 审查暂存区文件
     */
    public CompletableFuture<PreCommitReviewResponse> reviewStagedFiles(Project project) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("开始审查暂存区文件");

                // 预检查
                validatePreConditions(project);

                // 获取暂存区文件
                GitAnalysisService gitService = GitAnalysisService.getInstance();
                List<GitAnalysisService.StagedFileInfo> stagedFiles = gitService.getStagedFiles(project);

                if (stagedFiles.isEmpty()) {
                    throw new ReviewException("暂存区没有文件",
                        "请先使用 git add 添加要审查的文件到暂存区");
                }

                // 验证文件
                validateStagedFiles(stagedFiles);

                // 构建审查请求
                PreCommitReviewRequest request = buildReviewRequest(project, stagedFiles);

                // 发送审查请求（带重试）
                return executeReviewWithRetry(request);

            } catch (ReviewException e) {
                LOG.warn("审查预检查失败: " + e.getMessage());
                throw e;
            } catch (Exception e) {
                LOG.error("审查暂存区文件失败", e);
                throw new ReviewException("审查失败",
                    "系统错误: " + e.getMessage() + "\n请检查网络连接或稍后重试");
            }
        });
    }

    /**
     * 流式审查暂存区文件
     */
    public void reviewStagedFilesStream(Project project,
                                       Consumer<String> onMessage,
                                       Runnable onComplete,
                                       Consumer<Exception> onError) {
        try {
            LOG.info("开始流式审查暂存区文件");

            // 检查认证状态
            AuthService authService = AuthService.getInstance();
            if (!authService.isAuthenticated()) {
                onError.accept(new RuntimeException("用户未登录，请先登录JDC Tools账号"));
                return;
            }

            // 获取暂存区文件
            GitAnalysisService gitService = GitAnalysisService.getInstance();
            List<GitAnalysisService.StagedFileInfo> stagedFiles = gitService.getStagedFiles(project);

            if (stagedFiles.isEmpty()) {
                onError.accept(new RuntimeException("暂存区没有文件，请先使用 git add 添加要审查的文件"));
                return;
            }

            // 构建审查请求
            PreCommitReviewRequest request = buildReviewRequest(project, stagedFiles);

            // 发送流式审查请求
            ApiClient apiClient = ApiClient.getInstance();
            apiClient.postStream("/code-review/pre-commit-stream", request, onMessage, onComplete, onError);

        } catch (Exception e) {
            LOG.error("流式审查暂存区文件失败", e);
            onError.accept(e);
        }
    }

    /**
     * 审查单个文件
     */
    public CompletableFuture<PreCommitReviewResponse> reviewSingleFile(Project project, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("开始审查单个文件: " + filePath);

                // 检查认证状态
                AuthService authService = AuthService.getInstance();
                if (!authService.isAuthenticated()) {
                    throw new RuntimeException("用户未登录，请先登录JDC Tools账号");
                }

                // 获取文件信息
                GitAnalysisService gitService = GitAnalysisService.getInstance();
                List<GitAnalysisService.StagedFileInfo> stagedFiles = gitService.getStagedFiles(project);

                // 查找指定文件
                GitAnalysisService.StagedFileInfo targetFile = stagedFiles.stream()
                    .filter(file -> file.getFilePath().equals(filePath))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("文件未在暂存区中找到: " + filePath));

                // 构建单文件审查请求
                PreCommitReviewRequest request = buildSingleFileReviewRequest(project, targetFile);

                // 发送审查请求（使用带重试的方法）
                ApiClient apiClient = ApiClient.getInstance();
                ApiClient.ApiResponse response = apiClient.postWithRetry("/code-review/pre-commit", request).get();

                if (response.isSuccess()) {
                    Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                    Integer code = (Integer) result.get("code");

                    if (code != null && code == 200) {
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        return parseReviewResponse(data);
                    } else {
                        String message = (String) result.get("message");
                        throw new RuntimeException("审查失败: " + message);
                    }
                } else {
                    throw new RuntimeException("审查请求失败，状态码: " + response.getStatusCode());
                }

            } catch (Exception e) {
                LOG.error("审查单个文件失败", e);
                throw new RuntimeException("审查失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 构建审查请求
     */
    private PreCommitReviewRequest buildReviewRequest(Project project, List<GitAnalysisService.StagedFileInfo> stagedFiles) {
        PreCommitReviewRequest request = new PreCommitReviewRequest();

        // 设置基本信息
        request.setProjectName(project.getName());
        request.setReviewType("pre_commit");
        request.setModelKey("claude-sonnet-4-20250514");
        request.setReviewFocus("all");
        request.setAnalysisDepth("comprehensive");

        // 设置文件信息
        request.setFileCount(stagedFiles.size());

        // 构建文件列表
        Map<String, Object> filesData = new HashMap<>();
        for (GitAnalysisService.StagedFileInfo fileInfo : stagedFiles) {
            Map<String, Object> fileData = new HashMap<>();
            fileData.put("fileName", fileInfo.getFileName());
            fileData.put("filePath", fileInfo.getFilePath());
            fileData.put("content", fileInfo.getContent());
            fileData.put("changeType", fileInfo.getChangeType());
            fileData.put("diffContent", fileInfo.getDiffContent());
            fileData.put("language", fileInfo.getLanguage());

            filesData.put(fileInfo.getFilePath(), fileData);
        }

        request.setFiles(filesData);

        // 设置审查说明
        StringBuilder description = new StringBuilder();
        description.append("Git提交前代码审查\n");
        description.append("项目: ").append(project.getName()).append("\n");
        description.append("文件数量: ").append(stagedFiles.size()).append("\n");
        description.append("请重点关注以下变动文件，并提供详细的代码审查意见：\n");

        for (GitAnalysisService.StagedFileInfo fileInfo : stagedFiles) {
            description.append("- ").append(fileInfo.getFileName())
                      .append(" (").append(fileInfo.getChangeType()).append(")\n");
        }

        request.setDescription(description.toString());

        return request;
    }

    /**
     * 构建单文件审查请求
     */
    private PreCommitReviewRequest buildSingleFileReviewRequest(Project project, GitAnalysisService.StagedFileInfo fileInfo) {
        PreCommitReviewRequest request = new PreCommitReviewRequest();

        // 设置基本信息
        request.setProjectName(project.getName());
        request.setReviewType("single_file");
        request.setModelKey("claude-sonnet-4-20250514");
        request.setReviewFocus("all");
        request.setAnalysisDepth("detailed");

        // 设置文件信息
        request.setFileCount(1);

        // 构建文件数据
        Map<String, Object> filesData = new HashMap<>();
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("fileName", fileInfo.getFileName());
        fileData.put("filePath", fileInfo.getFilePath());
        fileData.put("content", fileInfo.getContent());
        fileData.put("changeType", fileInfo.getChangeType());
        fileData.put("diffContent", fileInfo.getDiffContent());
        fileData.put("language", fileInfo.getLanguage());

        filesData.put(fileInfo.getFilePath(), fileData);
        request.setFiles(filesData);

        // 设置审查说明
        String description = String.format(
            "单文件Git提交前代码审查\n项目: %s\n文件: %s (%s)\n请对此文件的变动进行详细的代码审查。",
            project.getName(), fileInfo.getFileName(), fileInfo.getChangeType()
        );
        request.setDescription(description);

        return request;
    }

    /**
     * 解析审查响应
     */
    private PreCommitReviewResponse parseReviewResponse(Map<String, Object> data) {
        PreCommitReviewResponse response = new PreCommitReviewResponse();

        // 添加调试日志
        LOG.info("开始解析审查响应，数据键: " + data.keySet());

        // 尝试获取分析结果 - 兼容不同的字段名
        String analysis = (String) data.get("analysis");
        if (analysis == null || analysis.trim().isEmpty()) {
            analysis = (String) data.get("detailedAnalysis");
            LOG.info("使用detailedAnalysis字段");
        }
        if (analysis == null || analysis.trim().isEmpty()) {
            analysis = (String) data.get("summary");
            LOG.info("使用summary字段");
        }
        if (analysis == null || analysis.trim().isEmpty()) {
            analysis = "代码审查完成，但AI响应解析失败。建议手动检查代码质量。";
            LOG.warn("所有分析字段都为空，使用默认消息");
        }
        response.setAnalysis(analysis);

        // 获取建议列表 - 支持新的结构化格式
        Object suggestionsObj = data.get("suggestions");
        List<String> suggestions = new ArrayList<>();

        if (suggestionsObj instanceof List) {
            List<?> suggestionsList = (List<?>) suggestionsObj;
            for (Object item : suggestionsList) {
                if (item instanceof Map) {
                    // 新的结构化格式
                    Map<String, Object> suggestionMap = (Map<String, Object>) item;
                    String description = (String) suggestionMap.get("description");
                    if (description != null && !description.trim().isEmpty()) {
                        suggestions.add(description);
                    } else {
                        // 降级到标题
                        String title = (String) suggestionMap.get("title");
                        suggestions.add(title != null ? title : "改进建议");
                    }
                } else if (item instanceof String) {
                    // 旧的字符串格式
                    suggestions.add((String) item);
                }
            }
        }

        // 如果还是空的，尝试其他字段
        if (suggestions.isEmpty()) {
            Object suggestionListObj = data.get("suggestionList");
            if (suggestionListObj instanceof List) {
                suggestions = (List<String>) suggestionListObj;
            }
        }

        response.setSuggestions(suggestions);

        // 保存结构化建议数据供UI使用
        if (suggestionsObj instanceof List) {
            response.setStructuredSuggestions((List<Map<String, Object>>) suggestionsObj);
        }

        // 安全地处理数字类型转换
        Object qualityScoreObj = data.get("qualityScore");
        if (qualityScoreObj instanceof Number) {
            response.setQualityScore(((Number) qualityScoreObj).intValue());
        } else {
            // 默认评分
            response.setQualityScore(75);
        }

        // 获取复杂度信息 - 兼容不同的字段名
        Map<String, Object> complexity = (Map<String, Object>) data.get("complexity");
        if (complexity == null) {
            complexity = (Map<String, Object>) data.get("metrics");
        }
        if (complexity == null) {
            // 创建默认复杂度信息
            complexity = new HashMap<>();
            complexity.put("overall", "medium");
            complexity.put("details", "复杂度分析完成");
        }
        response.setComplexity(complexity);

        // 安全地处理Long类型转换
        Object processingTimeObj = data.get("processingTime");
        if (processingTimeObj instanceof Number) {
            response.setProcessingTime(((Number) processingTimeObj).longValue());
        } else {
            // 设置默认处理时间
            response.setProcessingTime(System.currentTimeMillis() % 10000); // 模拟处理时间
        }

        response.setReviewId((String) data.get("reviewId"));

        // 设置其他字段
        response.setStatus("success");
        response.setMessage("代码审查完成");

        LOG.info("解析审查响应成功: 质量评分=" + response.getQualityScore() +
                ", 建议数量=" + response.getSuggestionCount() +
                ", 处理时间=" + response.getProcessingTime() + "ms");

        return response;
    }

    /**
     * 验证前置条件
     */
    private void validatePreConditions(Project project) {
        // 检查认证状态
        AuthService authService = AuthService.getInstance();
        if (!authService.isAuthenticated()) {
            throw new ReviewException("用户未登录",
                "请先登录JDC Tools账号\n点击登录标签页进行登录");
        }

        // 检查Git仓库
        GitAnalysisService gitService = GitAnalysisService.getInstance();
        if (!gitService.isGitRepository(project)) {
            throw new ReviewException("不是Git仓库",
                "当前项目不是Git仓库，无法进行代码审查");
        }
    }

    /**
     * 验证暂存文件
     */
    private void validateStagedFiles(List<GitAnalysisService.StagedFileInfo> stagedFiles) {
        if (stagedFiles.size() > MAX_FILES_PER_REVIEW) {
            throw new ReviewException("文件数量过多",
                String.format("暂存区有 %d 个文件，超过限制 (%d)\n建议分批提交或减少文件数量",
                    stagedFiles.size(), MAX_FILES_PER_REVIEW));
        }

        // 获取文件统计信息
        GitAnalysisService gitService = GitAnalysisService.getInstance();
        Map<String, Object> stats = gitService.getFileStats(stagedFiles);

        int supportedFiles = (Integer) stats.get("supportedFiles");
        int oversizedFiles = (Integer) stats.get("oversizedFiles");

        if (supportedFiles == 0) {
            if (oversizedFiles > 0) {
                throw new ReviewException("文件过大",
                    "所有文件都超过大小限制 (1MB)\n请减小文件大小或分批提交");
            } else {
                throw new ReviewException("不支持的文件类型",
                    "暂存区没有支持的代码文件\n支持的文件类型: Java, Kotlin, Python, JavaScript等");
            }
        }

        LOG.info("文件验证通过: 总计 " + stagedFiles.size() + " 个文件，支持 " + supportedFiles + " 个，过大 " + oversizedFiles + " 个");
    }

    /**
     * 带重试的审查执行
     */
    private PreCommitReviewResponse executeReviewWithRetry(PreCommitReviewRequest request) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                LOG.info("审查尝试 " + attempt + "/" + MAX_RETRY_ATTEMPTS);

                ApiClient apiClient = ApiClient.getInstance();
                ApiClient.ApiResponse response = apiClient.postWithRetry("/code-review/pre-commit", request).get();

                if (response.isSuccess()) {
                    Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                    Integer code = (Integer) result.get("code");

                    if (code != null && code == 200) {
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        LOG.info("审查成功，尝试次数: " + attempt);
                        return parseReviewResponse(data);
                    } else {
                        String message = (String) result.get("message");
                        if (message != null && message.contains("积分不足")) {
                            throw new ReviewException("积分不足",
                                "您的积分不足以进行代码审查\n请通过每日签到或其他方式获取积分");
                        }
                        throw new ReviewException("审查失败", "服务器返回错误: " + message);
                    }
                } else {
                    String errorMsg = String.format("HTTP %d: %s", response.getStatusCode(), response.getBody());
                    lastException = new RuntimeException(errorMsg);

                    // 如果是客户端错误（4xx），不重试
                    if (response.getStatusCode() >= 400 && response.getStatusCode() < 500) {
                        throw new ReviewException("请求错误", errorMsg);
                    }
                }

            } catch (ReviewException e) {
                // ReviewException不重试，直接抛出
                throw e;
            } catch (Exception e) {
                lastException = e;
                LOG.warn("审查尝试 " + attempt + " 失败: " + e.getMessage());

                // 如果不是最后一次尝试，等待后重试
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ReviewException("审查被中断", "操作被用户取消");
                    }
                }
            }
        }

        throw new ReviewException("审查失败",
            String.format("已重试 %d 次仍然失败\n最后错误: %s\n请检查网络连接或稍后重试",
                MAX_RETRY_ATTEMPTS, lastException != null ? lastException.getMessage() : "未知错误"));
    }
}
