package com.jdc.tools.precommit.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.jdc.tools.precommit.model.PreCommitReviewResponse;
import com.jdc.tools.precommit.service.AuthService;
import com.jdc.tools.precommit.service.ProjectGitService;
import com.jdc.tools.precommit.ui.panels.LoginPanel;
import com.jdc.tools.precommit.ui.panels.ReviewPanel;
import com.jdc.tools.precommit.ui.panels.HistoryPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Git提交前审查工具窗口
 * 现代化美观的用户界面
 */
public class PreCommitReviewToolWindow extends JBPanel<PreCommitReviewToolWindow> {

    private final Project project;
    private JBTabbedPane tabbedPane;
    private LoginPanel loginPanel;
    private ReviewPanel reviewPanel;
    private HistoryPanel historyPanel;

    public PreCommitReviewToolWindow(Project project) {
        super(new BorderLayout());
        this.project = project;
        initializeUI();
        checkAuthStatus();
    }

    /**
     * 初始化UI
     */
    private void initializeUI() {
        setBackground(JBColor.background());
        setBorder(JBUI.Borders.empty(8));

        // 创建标签页
        tabbedPane = new JBTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);

        // 登录面板
        loginPanel = new LoginPanel(project);
        loginPanel.setOnLoginSuccessCallback(this::onLoginSuccess);
        tabbedPane.addTab("🔐 登录", loginPanel);

        // 审查面板
        reviewPanel = new ReviewPanel(project);
        tabbedPane.addTab("🔍 代码审查", reviewPanel);

        // 历史面板
        historyPanel = new HistoryPanel(project);
        tabbedPane.addTab("📋 审查历史", historyPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // 添加底部状态栏
        add(createStatusBar(), BorderLayout.SOUTH);

        // 初始状态下只显示登录面板
        updateUIBasedOnAuthStatus(false);
    }

    /**
     * 创建状态栏
     */
    private JComponent createStatusBar() {
        JBPanel<?> statusBar = new JBPanel<>(new BorderLayout());
        statusBar.setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0));
        statusBar.setBackground(JBColor.background());

        // 左侧状态信息
        JBLabel statusLabel = new JBLabel();
        updateStatusLabel(statusLabel);
        statusBar.add(statusLabel, BorderLayout.WEST);

        // 右侧版本信息
        JBLabel versionLabel = new JBLabel("JDC Pre-Commit Review v1.0.0");
        versionLabel.setForeground(JBColor.GRAY);
        versionLabel.setFont(versionLabel.getFont().deriveFont(Font.PLAIN, 10f));
        statusBar.add(versionLabel, BorderLayout.EAST);

        return statusBar;
    }

    /**
     * 更新状态标签
     */
    private void updateStatusLabel(JBLabel statusLabel) {
        ProjectGitService gitService = ProjectGitService.getInstance(project);

        if (gitService.isGitRepository()) {
            String branch = gitService.getCurrentBranch();
            String text = "📁 " + project.getName();
            if (branch != null) {
                text += " | 🌿 " + branch;
            }
            statusLabel.setText(text);
            statusLabel.setForeground(JBColor.foreground());
        } else {
            statusLabel.setText("⚠️ 非Git仓库");
            statusLabel.setForeground(JBColor.RED);
        }
    }

    /**
     * 检查认证状态
     */
    private void checkAuthStatus() {
        AuthService authService = AuthService.getInstance();

        // 尝试自动登录
        authService.autoLogin().thenAccept(success -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                updateUIBasedOnAuthStatus(success);
                if (success) {
                    loginPanel.showLoginSuccess();
                }
            });
        });
    }

    /**
     * 根据认证状态更新UI
     */
    private void updateUIBasedOnAuthStatus(boolean isAuthenticated) {
        if (isAuthenticated) {
            // 已认证，显示所有功能
            tabbedPane.setEnabledAt(1, true); // 代码审查
            tabbedPane.setEnabledAt(2, true); // 审查历史
            tabbedPane.setSelectedIndex(1); // 切换到审查页
        } else {
            // 未认证，只显示登录页
            tabbedPane.setEnabledAt(1, false);
            tabbedPane.setEnabledAt(2, false);
            tabbedPane.setSelectedIndex(0); // 切换到登录页
        }
    }

    /**
     * 显示审查结果
     */
    public void displayReviewResult(PreCommitReviewResponse response) {
        reviewPanel.displayResult(response);
        tabbedPane.setSelectedIndex(1); // 切换到审查结果页
    }

    /**
     * 刷新历史记录
     */
    public void refreshHistory() {
        historyPanel.refreshHistory();
    }

    /**
     * 获取内容组件
     */
    public JComponent getContent() {
        return this;
    }

    /**
     * 登录成功回调
     */
    public void onLoginSuccess() {
        updateUIBasedOnAuthStatus(true);
        reviewPanel.refreshStagedFiles();
    }

    /**
     * 登出回调
     */
    public void onLogout() {
        updateUIBasedOnAuthStatus(false);
        reviewPanel.clearResults();
        historyPanel.clearHistory();
    }
}
