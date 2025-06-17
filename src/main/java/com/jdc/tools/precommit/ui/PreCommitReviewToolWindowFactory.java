package com.jdc.tools.precommit.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Git提交前审查工具窗口工厂
 */
public class PreCommitReviewToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建工具窗口内容
        PreCommitReviewToolWindow toolWindowContent = new PreCommitReviewToolWindow(project);
        
        // 创建内容并添加到工具窗口
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(toolWindowContent.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
