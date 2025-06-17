package com.jdc.tools.precommit.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jdc.tools.precommit.model.PreCommitReviewResponse;
import com.jdc.tools.precommit.service.AuthService;
import com.jdc.tools.precommit.service.GitAnalysisService;
import com.jdc.tools.precommit.service.PreCommitReviewService;
import com.jdc.tools.precommit.service.ProjectGitService;
import com.jdc.tools.precommit.ui.PreCommitReviewToolWindow;
import com.jdc.tools.precommit.ui.FileSelectionDialog;
import com.jdc.tools.precommit.util.FileTypeFilter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * 审查暂存文件Action
 * 审查git add的所有文件
 */
public class ReviewStagedFilesAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // 检查是否为Git仓库
        ProjectGitService projectGitService = ProjectGitService.getInstance(project);
        if (!projectGitService.isGitRepository()) {
            Messages.showWarningDialog(
                "当前项目不是Git仓库，无法进行提交前审查。",
                "Git仓库检查");
            return;
        }

        // 检查认证状态
        AuthService authService = AuthService.getInstance();
        if (!authService.isAuthenticated()) {
            // 显示登录提示并打开工具窗口
            int result = Messages.showYesNoDialog(
                project,
                "需要登录JDC Tools账号才能使用代码审查功能。\n是否现在登录？",
                "需要登录",
                Messages.getQuestionIcon()
            );

            if (result == Messages.YES) {
                showToolWindow(project);
            }
            return;
        }

        // 检查暂存区是否有文件
        GitAnalysisService gitService = GitAnalysisService.getInstance();
        var stagedFiles = gitService.getStagedFiles(project);
        if (stagedFiles.isEmpty()) {
            Messages.showInfoMessage(
                "暂存区没有文件。\n请先使用 'git add' 命令添加要审查的文件。",
                "暂存区为空");
            return;
        }

        // 转换为VirtualFile列表，并过滤代码文件
        List<VirtualFile> virtualFiles = new ArrayList<>();
        int totalFiles = stagedFiles.size();
        int filteredFiles = 0;

        for (var stagedFile : stagedFiles) {
            VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(stagedFile.getFilePath());
            if (vf != null) {
                if (FileTypeFilter.isCodeFile(vf)) {
                    virtualFiles.add(vf);
                } else {
                    filteredFiles++;
                    // 记录过滤的文件（使用System.out.println代替LOG）
                    System.out.println("过滤非代码文件: " + vf.getName() + " (" + FileTypeFilter.getFileTypeDescription(vf) + ")");
                }
            }
        }

        // 显示过滤信息
        if (filteredFiles > 0) {
            String message = String.format(
                "检测到 %d 个暂存文件，其中 %d 个为代码文件，%d 个非代码文件已被过滤。\n\n" +
                "过滤的文件类型包括：二进制文件、锁文件、缓存文件等。\n" +
                "只有代码文件（如 .java、.js、.py 等）可以进行审查。",
                totalFiles, virtualFiles.size(), filteredFiles
            );

            int choice = Messages.showYesNoDialog(
                message + "\n\n是否继续审查代码文件？",
                "文件过滤提示",
                "继续审查",
                "取消",
                Messages.getQuestionIcon()
            );

            if (choice != Messages.YES) {
                return;
            }
        }

        if (virtualFiles.isEmpty()) {
            Messages.showInfoMessage(
                "暂存区中没有可审查的代码文件。\n\n" +
                "支持的文件类型包括：\n" +
                "• 编程语言：.java、.kt、.py、.js、.ts、.go、.rs 等\n" +
                "• 前端文件：.html、.css、.vue、.jsx、.tsx 等\n" +
                "• 配置文件：.xml、.json、.yaml、.properties 等\n" +
                "• 脚本文件：.sh、.sql、.md 等",
                "无可审查文件");
            return;
        }

        // 显示文件选择对话框
        FileSelectionDialog dialog = new FileSelectionDialog(project, virtualFiles);
        if (!dialog.showAndGet()) {
            return; // 用户取消了选择
        }

        var selectedFiles = dialog.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            Messages.showInfoMessage(
                "没有选择任何文件进行审查。",
                "未选择文件");
            return;
        }

        // 显示工具窗口
        showToolWindow(project);

        // 在后台执行审查
        performReview(project, selectedFiles);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        // 只有在Git仓库中才启用
        ProjectGitService projectGitService = ProjectGitService.getInstance(project);
        boolean isGitRepo = projectGitService.isGitRepository();
        e.getPresentation().setEnabledAndVisible(isGitRepo);

        if (isGitRepo) {
            // 更新Action文本，显示当前分支
            String currentBranch = projectGitService.getCurrentBranch();
            if (currentBranch != null) {
                e.getPresentation().setText("审查暂存文件 (" + currentBranch + ")");
            } else {
                e.getPresentation().setText("审查暂存文件");
            }
        }
    }

    /**
     * 执行审查
     */
    private void performReview(Project project, java.util.List<VirtualFile> selectedFiles) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "正在审查选定文件...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("正在分析选定文件...");

                try {
                    indicator.setText("正在审查 " + selectedFiles.size() + " 个文件，正在发送审查请求...");

                    // 执行审查
                    PreCommitReviewService reviewService = PreCommitReviewService.getInstance();
                    PreCommitReviewResponse response = reviewService.reviewSelectedFiles(project, selectedFiles).get();

                    // 在EDT中更新UI
                    ApplicationManager.getApplication().invokeLater(() -> {
                        updateToolWindowWithResult(project, response);

                        // 显示完成通知
                        String message = String.format(
                            "代码审查完成！\n质量评分: %d/100 (%s)\n建议数量: %d条",
                            response.getQualityScore(),
                            response.getQualityLevel(),
                            response.getSuggestionCount()
                        );

                        Messages.showInfoMessage(message, "审查完成");
                    });

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        String errorMessage = "审查失败: " + ex.getMessage();
                        Messages.showErrorDialog(errorMessage, "审查错误");
                    });
                }
            }
        });
    }

    /**
     * 显示工具窗口
     */
    private void showToolWindow(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("JDC Pre-Commit Review");

        if (toolWindow != null) {
            toolWindow.show();
            toolWindow.activate(null);
        }
    }

    /**
     * 更新工具窗口显示审查结果
     */
    private void updateToolWindowWithResult(Project project, PreCommitReviewResponse response) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("JDC Pre-Commit Review");

        if (toolWindow != null && toolWindow.getContentManager().getContentCount() > 0) {
            // 获取工具窗口的内容
            var content = toolWindow.getContentManager().getContent(0);
            if (content != null) {
                var component = content.getComponent();
                if (component instanceof PreCommitReviewToolWindow) {
                    PreCommitReviewToolWindow toolWindowPanel = (PreCommitReviewToolWindow) component;
                    // 显示审查结果
                    toolWindowPanel.displayReviewResult(response);
                }
            }
        }
    }
}
