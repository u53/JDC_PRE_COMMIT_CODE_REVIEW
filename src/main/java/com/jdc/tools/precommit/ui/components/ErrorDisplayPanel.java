package com.jdc.tools.precommit.ui.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import com.jdc.tools.precommit.exception.ReviewException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 错误显示面板
 * 提供用户友好的错误信息显示和处理选项
 */
public class ErrorDisplayPanel extends JBPanel<ErrorDisplayPanel> {

    private JBLabel errorIconLabel;
    private JBLabel errorTitleLabel;
    private JTextArea errorDetailsArea;
    private JBPanel<?> solutionPanel;
    private JButton retryButton;
    private JButton helpButton;
    private JButton reportButton;
    private JCheckBox showDetailsCheckBox;
    private JBPanel<?> detailsPanel;

    private String errorTitle;
    private String errorDetails;
    private Runnable retryAction;

    public ErrorDisplayPanel() {
        super(new BorderLayout());
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        setBackground(JBColor.background());
        setBorder(JBUI.Borders.empty(20));

        // 错误图标
        errorIconLabel = new JBLabel("❌");
        errorIconLabel.setFont(errorIconLabel.getFont().deriveFont(48f));
        errorIconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 错误标题
        errorTitleLabel = new JBLabel("发生错误");
        errorTitleLabel.setFont(errorTitleLabel.getFont().deriveFont(Font.BOLD, 16f));
        errorTitleLabel.setForeground(new Color(220, 53, 69));
        errorTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 错误详情
        errorDetailsArea = new JTextArea();
        errorDetailsArea.setEditable(false);
        errorDetailsArea.setLineWrap(true);
        errorDetailsArea.setWrapStyleWord(true);
        errorDetailsArea.setBackground(new Color(248, 249, 250));
        errorDetailsArea.setForeground(JBColor.foreground());
        errorDetailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        errorDetailsArea.setBorder(JBUI.Borders.empty(10));

        // 解决方案面板
        solutionPanel = new JBPanel<>();
        solutionPanel.setLayout(new BoxLayout(solutionPanel, BoxLayout.Y_AXIS));
        solutionPanel.setBackground(JBColor.background());

        // 显示详情复选框
        showDetailsCheckBox = new JCheckBox("显示技术详情", false);
        showDetailsCheckBox.addActionListener(e -> toggleDetailsVisibility());

        // 详情面板
        detailsPanel = new JBPanel<>(new BorderLayout());
        detailsPanel.setBackground(JBColor.background());
        detailsPanel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.GRAY, 1),
            JBUI.Borders.empty(10)
        ));
        detailsPanel.setVisible(false);

        JBScrollPane detailsScrollPane = new JBScrollPane(errorDetailsArea);
        detailsScrollPane.setPreferredSize(new Dimension(0, 150));
        detailsScrollPane.setBorder(null);
        detailsPanel.add(detailsScrollPane, BorderLayout.CENTER);

        // 操作按钮
        retryButton = new JButton("🔄 重试");
        retryButton.setToolTipText("重新尝试执行操作");
        retryButton.addActionListener(new RetryActionListener());

        helpButton = new JButton("❓ 获取帮助");
        helpButton.setToolTipText("查看帮助文档");
        helpButton.addActionListener(new HelpActionListener());

        reportButton = new JButton("🐛 报告问题");
        reportButton.setToolTipText("向开发团队报告此问题");
        reportButton.addActionListener(new ReportActionListener());
    }

    private void setupLayout() {
        // 主内容面板
        JBPanel<?> contentPanel = new JBPanel<>(new BorderLayout());
        contentPanel.setBackground(JBColor.background());

        // 顶部 - 图标和标题
        JBPanel<?> headerPanel = new JBPanel<>(new BorderLayout());
        headerPanel.setBackground(JBColor.background());
        headerPanel.setBorder(JBUI.Borders.empty(0, 0, 20, 0));

        headerPanel.add(errorIconLabel, BorderLayout.NORTH);
        headerPanel.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        headerPanel.add(errorTitleLabel, BorderLayout.SOUTH);

        contentPanel.add(headerPanel, BorderLayout.NORTH);

        // 中间 - 解决方案和详情
        JBPanel<?> middlePanel = new JBPanel<>(new BorderLayout());
        middlePanel.setBackground(JBColor.background());

        middlePanel.add(solutionPanel, BorderLayout.NORTH);
        middlePanel.add(Box.createVerticalStrut(15), BorderLayout.CENTER);

        JBPanel<?> detailsTogglePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        detailsTogglePanel.setBackground(JBColor.background());
        detailsTogglePanel.add(showDetailsCheckBox);

        JBPanel<?> detailsContainer = new JBPanel<>(new BorderLayout());
        detailsContainer.setBackground(JBColor.background());
        detailsContainer.add(detailsTogglePanel, BorderLayout.NORTH);
        detailsContainer.add(detailsPanel, BorderLayout.CENTER);

        middlePanel.add(detailsContainer, BorderLayout.SOUTH);
        contentPanel.add(middlePanel, BorderLayout.CENTER);

        // 底部 - 操作按钮
        JBPanel<?> buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(JBColor.background());
        buttonPanel.setBorder(JBUI.Borders.empty(20, 0, 0, 0));

        buttonPanel.add(retryButton);
        buttonPanel.add(helpButton);
        buttonPanel.add(reportButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * 显示错误信息
     */
    public void showError(String title, String details, Runnable retryAction) {
        this.errorTitle = title;
        this.errorDetails = details;
        this.retryAction = retryAction;

        errorTitleLabel.setText(title);
        errorDetailsArea.setText(details);

        // 清空并重新生成解决方案
        generateSolutions();

        // 启用/禁用重试按钮
        retryButton.setEnabled(retryAction != null);

        revalidate();
        repaint();
    }

    /**
     * 显示ReviewException错误
     */
    public void showError(ReviewException exception, Runnable retryAction) {
        showError(exception.getTitle(), exception.getDetails(), retryAction);
    }

    /**
     * 生成解决方案建议
     */
    private void generateSolutions() {
        solutionPanel.removeAll();

        if (errorTitle == null) return;

        JBLabel solutionTitleLabel = new JBLabel("💡 解决建议:");
        solutionTitleLabel.setFont(solutionTitleLabel.getFont().deriveFont(Font.BOLD, 14f));
        solutionTitleLabel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        solutionPanel.add(solutionTitleLabel);

        // 根据错误类型生成不同的解决方案
        if (errorTitle.contains("未登录") || errorTitle.contains("认证")) {
            addSolution("1. 点击 \"登录\" 标签页进行账号登录");
            addSolution("2. 确保您有有效的JDC Tools账号");
            addSolution("3. 检查网络连接是否正常");
        } else if (errorTitle.contains("暂存区") || errorTitle.contains("文件")) {
            addSolution("1. 使用 git add <文件名> 添加文件到暂存区");
            addSolution("2. 确保文件已保存并且是支持的代码文件");
            addSolution("3. 检查文件大小是否超过限制 (1MB)");
        } else if (errorTitle.contains("网络") || errorTitle.contains("连接")) {
            addSolution("1. 检查网络连接是否正常");
            addSolution("2. 确认防火墙没有阻止连接");
            addSolution("3. 稍后重试或联系管理员");
        } else if (errorTitle.contains("积分")) {
            addSolution("1. 通过每日签到获取积分");
            addSolution("2. 完成平台任务获得奖励积分");
            addSolution("3. 联系管理员充值积分");
        } else if (errorTitle.contains("文件数量") || errorTitle.contains("过多")) {
            addSolution("1. 减少暂存区文件数量 (建议少于20个)");
            addSolution("2. 分批提交代码");
            addSolution("3. 在配置中调整最大文件数限制");
        } else {
            addSolution("1. 检查网络连接和服务器状态");
            addSolution("2. 重启IDE并重新尝试");
            addSolution("3. 如果问题持续，请联系技术支持");
        }

        solutionPanel.revalidate();
        solutionPanel.repaint();
    }

    /**
     * 添加解决方案条目
     */
    private void addSolution(String solution) {
        JBLabel solutionLabel = new JBLabel(solution);
        solutionLabel.setFont(solutionLabel.getFont().deriveFont(13f));
        solutionLabel.setBorder(JBUI.Borders.empty(3, 20, 3, 0));
        solutionPanel.add(solutionLabel);
    }

    /**
     * 切换详情显示
     */
    private void toggleDetailsVisibility() {
        boolean showDetails = showDetailsCheckBox.isSelected();
        detailsPanel.setVisible(showDetails);
        revalidate();
        repaint();
    }

    /**
     * 重试操作监听器
     */
    private class RetryActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (retryAction != null) {
                retryAction.run();
            }
        }
    }

    /**
     * 帮助操作监听器
     */
    private class HelpActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO: 打开帮助文档
            JOptionPane.showMessageDialog(ErrorDisplayPanel.this,
                "帮助文档功能开发中...\n\n" +
                "您可以:\n" +
                "1. 查看官方文档: https://www.jdctools.com.cn/docs\n" +
                "2. 联系技术支持: support@jdctools.com.cn\n" +
                "3. 加入用户群获取帮助",
                "获取帮助", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 报告问题监听器
     */
    private class ReportActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO: 实现问题报告功能
            String reportContent = String.format(
                "错误标题: %s\n\n错误详情:\n%s\n\n" +
                "请描述您遇到问题时的操作步骤:\n\n\n" +
                "系统信息:\n" +
                "- IDE版本: %s\n" +
                "- 插件版本: 1.0.0\n" +
                "- 操作系统: %s",
                errorTitle != null ? errorTitle : "未知错误",
                errorDetails != null ? errorDetails : "无详细信息",
                System.getProperty("java.version"),
                System.getProperty("os.name")
            );

            JTextArea reportArea = new JTextArea(reportContent, 15, 50);
            reportArea.setLineWrap(true);
            reportArea.setWrapStyleWord(true);

            JScrollPane scrollPane = new JScrollPane(reportArea);
            
            int result = JOptionPane.showConfirmDialog(ErrorDisplayPanel.this,
                scrollPane, "报告问题", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                JOptionPane.showMessageDialog(ErrorDisplayPanel.this,
                    "感谢您的反馈！\n问题报告已记录，我们会尽快处理。",
                    "报告已提交", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}
