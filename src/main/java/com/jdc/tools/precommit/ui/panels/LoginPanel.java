package com.jdc.tools.precommit.ui.panels;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import com.jdc.tools.precommit.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * 登录面板
 * 美观的登录界面
 */
public class LoginPanel extends JBPanel<LoginPanel> {

    private final Project project;
    private JButton loginButton;
    private JButton logoutButton;
    private JBLabel statusLabel;
    private JBPanel<?> loginFormPanel;
    private JBPanel<?> userInfoPanel;
    private JBLabel userNameLabel;
    private JBLabel userEmailLabel;
    private Runnable onLoginSuccessCallback;

    public LoginPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        initializeUI();
        updateLoginStatus();
    }

    /**
     * 初始化UI
     */
    private void initializeUI() {
        setBackground(JBColor.background());
        setBorder(JBUI.Borders.empty(20));

        // 创建主容器
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBackground(JBColor.background());

        // 标题区域
        JBPanel<?> titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // 内容区域
        JBPanel<?> contentPanel = new JBPanel<>(new CardLayout());
        contentPanel.setBackground(JBColor.background());
        contentPanel.setBorder(JBUI.Borders.empty(20, 0, 0, 0));

        // 登录表单
        loginFormPanel = createLoginForm();
        contentPanel.add(loginFormPanel, "LOGIN_FORM");

        // 用户信息面板
        userInfoPanel = createUserInfoPanel();
        contentPanel.add(userInfoPanel, "USER_INFO");

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 状态区域
        JBPanel<?> statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * 创建标题面板
     */
    private JBPanel<?> createTitlePanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBackground(JBColor.background());

        // 主标题
        JBLabel titleLabel = new JBLabel("JDC Pre-Commit Review");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(JBColor.foreground());
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 副标题
        JBLabel subtitleLabel = new JBLabel("Git提交前智能代码审查");
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 12f));
        subtitleLabel.setForeground(JBColor.GRAY);
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(titleLabel, BorderLayout.CENTER);
        panel.add(subtitleLabel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 创建登录表单
     */
    private JBPanel<?> createLoginForm() {
        JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
        panel.setBackground(JBColor.background());
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.border(), 1),
            JBUI.Borders.empty(20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(10);
        gbc.anchor = GridBagConstraints.CENTER;

        // 说明文字
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 1;
        JBLabel descLabel = new JBLabel("<html><div style='text-align: center; width: 300px;'>" +
                "🔐 <strong>安全OAuth授权登录</strong><br/><br/>" +
                "点击下方按钮将自动打开浏览器到：<br/>" +
                "<span style='color: #0066cc; font-weight: bold;'>https://www.jdctools.com.cn/plugin-auth</span><br/><br/>" +
                "🛡️ 全程安全加密，无需在插件内输入密码" +
                "</div></html>");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 12f));
        descLabel.setForeground(JBColor.GRAY);
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(descLabel, gbc);

        // OAuth登录按钮
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(20, 10, 10, 10);
        loginButton = new JButton("🚀 打开浏览器授权登录");
        loginButton.setFont(loginButton.getFont().deriveFont(Font.BOLD, 14f));
        loginButton.setPreferredSize(new Dimension(300, 40));
        loginButton.addActionListener(new OAuthLoginActionListener());
        panel.add(loginButton, gbc);

        // 帮助信息
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = JBUI.insets(15, 10, 10, 10);
        JBLabel helpLabel = new JBLabel("<html><div style='text-align: center; width: 300px;'>" +
                "💡 首次使用需要注册JDC Tools账号<br/>" +
                "🆘 遇到问题请访问：<span style='color: #0066cc;'>https://www.jdctools.com.cn/support</span>" +
                "</div></html>");
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.PLAIN, 11f));
        helpLabel.setForeground(JBColor.GRAY);
        helpLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(helpLabel, gbc);

        return panel;
    }

    /**
     * 创建用户信息面板
     */
    private JBPanel<?> createUserInfoPanel() {
        JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
        panel.setBackground(JBColor.background());
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.GREEN, 2),
            JBUI.Borders.empty(20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.WEST;

        // 登录成功图标
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        JBLabel successIcon = new JBLabel("✅ 登录成功");
        successIcon.setFont(successIcon.getFont().deriveFont(Font.BOLD, 16f));
        successIcon.setForeground(JBColor.GREEN);
        successIcon.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(successIcon, gbc);

        // 用户名
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        JBLabel userLabel = new JBLabel("用户:");
        userLabel.setFont(userLabel.getFont().deriveFont(Font.BOLD));
        panel.add(userLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        userNameLabel = new JBLabel("-");
        panel.add(userNameLabel, gbc);

        // 邮箱
        gbc.gridx = 0; gbc.gridy = 2;
        JBLabel emailLabel = new JBLabel("邮箱:");
        emailLabel.setFont(emailLabel.getFont().deriveFont(Font.BOLD));
        panel.add(emailLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        userEmailLabel = new JBLabel("-");
        panel.add(userEmailLabel, gbc);

        // 登出按钮
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(15, 5, 5, 5);
        logoutButton = new JButton("🚪 登出");
        logoutButton.setFont(logoutButton.getFont().deriveFont(Font.BOLD, 14f));
        logoutButton.setPreferredSize(new Dimension(0, 35));
        logoutButton.addActionListener(e -> logout());
        panel.add(logoutButton, gbc);

        return panel;
    }

    /**
     * 创建状态面板
     */
    private JBPanel<?> createStatusPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBackground(JBColor.background());
        panel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));

        statusLabel = new JBLabel("请登录JDC Tools账号");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        statusLabel.setForeground(JBColor.GRAY);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(statusLabel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 更新登录状态
     */
    private void updateLoginStatus() {
        AuthService authService = AuthService.getInstance();
        boolean isAuthenticated = authService.isAuthenticated();

        JBPanel<?> mainPanel = (JBPanel<?>) getComponent(0);
        JBPanel<?> contentPanel = (JBPanel<?>) mainPanel.getComponent(1);
        CardLayout cardLayout = (CardLayout) contentPanel.getLayout();

        if (isAuthenticated) {
            // 显示用户信息
            cardLayout.show(contentPanel, "USER_INFO");

            // 更新用户信息
            Map<String, Object> userInfo = authService.getCurrentUserInfo();
            if (userInfo != null) {
                userNameLabel.setText((String) userInfo.get("username"));
                userEmailLabel.setText((String) userInfo.get("email"));
            }

            statusLabel.setText("✅ 已登录，可以使用代码审查功能");
            statusLabel.setForeground(JBColor.GREEN);
        } else {
            // 显示登录表单
            cardLayout.show(contentPanel, "LOGIN_FORM");
            statusLabel.setText("⚠️ 请登录JDC Tools账号");
            statusLabel.setForeground(JBColor.GRAY);
        }
    }

    /**
     * 设置登录成功回调
     */
    public void setOnLoginSuccessCallback(Runnable callback) {
        this.onLoginSuccessCallback = callback;
    }

    /**
     * 显示登录成功
     */
    public void showLoginSuccess() {
        updateLoginStatus();
    }

    /**
     * 登出
     */
    private void logout() {
        // 禁用登出按钮，防止重复点击
        logoutButton.setEnabled(false);
        logoutButton.setText("🔄 登出中...");

        AuthService authService = AuthService.getInstance();

        // 异步调用退出登录
        authService.logout().thenAccept(success -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                // 恢复按钮状态
                logoutButton.setEnabled(true);
                logoutButton.setText("🚪 登出");

                // 更新UI状态
                updateLoginStatus();

                if (success) {
                    Messages.showInfoMessage("已成功登出，token已失效", "登出成功");
                } else {
                    Messages.showWarningDialog("登出过程中出现问题，但本地状态已清除", "登出完成");
                }
            });
        }).exceptionally(throwable -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                // 恢复按钮状态
                logoutButton.setEnabled(true);
                logoutButton.setText("🚪 登出");

                // 更新UI状态
                updateLoginStatus();

                Messages.showErrorDialog("登出失败: " + throwable.getMessage(), "登出错误");
            });
            return null;
        });
    }

    /**
     * OAuth登录Action监听器
     */
    private class OAuthLoginActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 禁用登录按钮
            loginButton.setEnabled(false);
            loginButton.setText("🔄 授权中...");

            // 异步OAuth登录
            AuthService authService = AuthService.getInstance();
            authService.loginWithOAuth().thenAccept(success -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("🚀 打开浏览器授权登录");

                    if (success) {
                        updateLoginStatus();
                        Messages.showInfoMessage("授权登录成功！", "登录成功");

                        // 通知父窗口登录成功
                        if (onLoginSuccessCallback != null) {
                            onLoginSuccessCallback.run();
                        }
                    } else {
                        Messages.showErrorDialog("授权登录失败，请重试", "登录失败");
                    }
                });
            });
        }
    }
}
