package com.jdc.tools.precommit.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jdc.tools.precommit.service.ProjectGitService;
import org.jetbrains.annotations.NotNull;

/**
 * 打开工具窗口Action
 * 显示Git提交前审查工具窗口
 */
public class OpenToolWindowAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        showToolWindow(project);
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
}
