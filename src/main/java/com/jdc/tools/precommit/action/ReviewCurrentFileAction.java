package com.jdc.tools.precommit.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jdc.tools.precommit.model.PreCommitReviewResponse;
import com.jdc.tools.precommit.service.AuthService;
import com.jdc.tools.precommit.service.PreCommitReviewService;
import com.jdc.tools.precommit.service.ProjectGitService;
import com.jdc.tools.precommit.ui.PreCommitReviewToolWindow;
import org.jetbrains.annotations.NotNull;

/**
 * 审查当前文件Action
 * 审查当前打开的文件（如果在暂存区中）
 */
public class ReviewCurrentFileAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) {
            Messages.showWarningDialog("请先选择要审查的文件。", "文件选择");
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

        // 显示工具窗口
        showToolWindow(project);

        // 在后台执行审查
        performReview(project, file.getPath());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || file == null || file.isDirectory()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        // 只有在Git仓库中才启用
        ProjectGitService projectGitService = ProjectGitService.getInstance(project);
        boolean isGitRepo = projectGitService.isGitRepository();
        e.getPresentation().setEnabledAndVisible(isGitRepo);

        if (isGitRepo) {
            // 更新Action文本，显示文件名
            String fileName = file.getName();
            e.getPresentation().setText("审查当前文件 (" + fileName + ")");
        }
    }

    /**
     * 执行审查
     */
    private void performReview(Project project, String filePath) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "正在审查当前文件...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("正在分析文件: " + filePath);

                try {
                    // 执行单文件审查
                    PreCommitReviewService reviewService = PreCommitReviewService.getInstance();
                    PreCommitReviewResponse response = reviewService.reviewSingleFile(project, filePath).get();

                    // 在EDT中更新UI
                    ApplicationManager.getApplication().invokeLater(() -> {
                        updateToolWindowWithResult(project, response);

                        // 显示完成通知
                        String message = String.format(
                            "文件审查完成！\n质量评分: %d/100 (%s)\n建议数量: %d条",
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
