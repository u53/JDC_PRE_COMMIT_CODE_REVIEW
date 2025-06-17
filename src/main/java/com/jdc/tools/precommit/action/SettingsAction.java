package com.jdc.tools.precommit.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.jdc.tools.precommit.service.AuthService;
import org.jetbrains.annotations.NotNull;

/**
 * 设置Action
 * 打开插件设置对话框
 */
public class SettingsAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // 显示设置信息
        showSettingsInfo(project);
    }
    
    /**
     * 显示设置信息
     */
    private void showSettingsInfo(Project project) {
        AuthService authService = AuthService.getInstance();
        
        StringBuilder info = new StringBuilder();
        info.append("JDC Pre-Commit Review 设置\n\n");
        
        // 认证状态
        if (authService.isAuthenticated()) {
            var userInfo = authService.getCurrentUserInfo();
            if (userInfo != null) {
                info.append("登录状态: 已登录\n");
                info.append("用户名: ").append(userInfo.get("username")).append("\n");
                info.append("邮箱: ").append(userInfo.get("email")).append("\n");
            } else {
                info.append("登录状态: 已登录（用户信息加载中）\n");
            }
        } else {
            info.append("登录状态: 未登录\n");
        }
        
        info.append("\n");
        info.append("后端服务: https://www.jdctools.com.cn/api\n");
        info.append("AI模型: claude-sonnet-4-20250514\n");
        info.append("插件版本: 1.0.0\n");
        
        info.append("\n");
        info.append("快捷键:\n");
        info.append("- Ctrl+Shift+R: 审查暂存文件\n");
        info.append("- Ctrl+Shift+F: 审查当前文件\n");
        info.append("- Ctrl+Shift+P: 打开审查窗口\n");
        
        Messages.showInfoMessage(info.toString(), "插件设置");
    }
}
