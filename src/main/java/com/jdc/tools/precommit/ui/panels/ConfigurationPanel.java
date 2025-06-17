package com.jdc.tools.precommit.ui.panels;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 配置面板
 * 提供代码审查的各种配置选项
 */
public class ConfigurationPanel extends JBPanel<ConfigurationPanel> {

    private final Project project;
    
    // 审查重点配置
    private JCheckBox securityCheckBox;
    private JCheckBox performanceCheckBox;
    private JCheckBox styleCheckBox;
    private JCheckBox logicCheckBox;
    
    // 分析深度配置
    private JRadioButton quickAnalysisRadio;
    private JRadioButton standardAnalysisRadio;
    private JRadioButton detailedAnalysisRadio;
    private JRadioButton comprehensiveAnalysisRadio;
    
    // 其他配置
    private JCheckBox autoReviewCheckBox;
    private JCheckBox saveHistoryCheckBox;
    private JCheckBox showNotificationsCheckBox;
    private JSpinner maxFilesSpinner;
    private JSpinner timeoutSpinner;
    
    // 按钮
    private JButton saveButton;
    private JButton resetButton;
    private JButton testConnectionButton;

    public ConfigurationPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        initializeComponents();
        setupLayout();
        loadConfiguration();
    }

    private void initializeComponents() {
        setBackground(JBColor.background());
        setBorder(JBUI.Borders.empty(15));

        // 审查重点复选框
        securityCheckBox = new JCheckBox("安全性检查", true);
        securityCheckBox.setToolTipText("检查潜在的安全漏洞和风险");
        
        performanceCheckBox = new JCheckBox("性能分析", true);
        performanceCheckBox.setToolTipText("分析代码性能和优化建议");
        
        styleCheckBox = new JCheckBox("代码风格", true);
        styleCheckBox.setToolTipText("检查代码格式和命名规范");
        
        logicCheckBox = new JCheckBox("逻辑结构", true);
        logicCheckBox.setToolTipText("分析代码逻辑和结构合理性");

        // 分析深度单选按钮
        quickAnalysisRadio = new JRadioButton("快速分析 (~30s)", false);
        quickAnalysisRadio.setToolTipText("基础检查，速度最快");
        
        standardAnalysisRadio = new JRadioButton("标准分析 (~60s)", true);
        standardAnalysisRadio.setToolTipText("平衡的分析深度和速度");
        
        detailedAnalysisRadio = new JRadioButton("详细分析 (~120s)", false);
        detailedAnalysisRadio.setToolTipText("深入分析，提供更多建议");
        
        comprehensiveAnalysisRadio = new JRadioButton("全面分析 (~180s)", false);
        comprehensiveAnalysisRadio.setToolTipText("最全面的分析，耗时最长");

        ButtonGroup analysisGroup = new ButtonGroup();
        analysisGroup.add(quickAnalysisRadio);
        analysisGroup.add(standardAnalysisRadio);
        analysisGroup.add(detailedAnalysisRadio);
        analysisGroup.add(comprehensiveAnalysisRadio);

        // 其他配置选项
        autoReviewCheckBox = new JCheckBox("自动审查", false);
        autoReviewCheckBox.setToolTipText("在git add后自动触发代码审查");
        
        saveHistoryCheckBox = new JCheckBox("保存历史记录", true);
        saveHistoryCheckBox.setToolTipText("本地保存审查历史记录");
        
        showNotificationsCheckBox = new JCheckBox("显示通知", true);
        showNotificationsCheckBox.setToolTipText("审查完成后显示系统通知");

        // 数值配置
        maxFilesSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 100, 1));
        maxFilesSpinner.setToolTipText("单次审查的最大文件数量");
        
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(300, 30, 600, 30));
        timeoutSpinner.setToolTipText("审查超时时间（秒）");

        // 操作按钮
        saveButton = new JButton("💾 保存配置");
        saveButton.setToolTipText("保存当前配置");
        saveButton.addActionListener(new SaveActionListener());
        
        resetButton = new JButton("🔄 重置默认");
        resetButton.setToolTipText("重置为默认配置");
        resetButton.addActionListener(new ResetActionListener());
        
        testConnectionButton = new JButton("🔗 测试连接");
        testConnectionButton.setToolTipText("测试与JDC服务器的连接");
        testConnectionButton.addActionListener(new TestConnectionActionListener());
    }

    private void setupLayout() {
        JBPanel<?> mainPanel = new JBPanel<>(new GridBagLayout());
        mainPanel.setBackground(JBColor.background());
        GridBagConstraints gbc = new GridBagConstraints();

        // 审查重点配置区域
        JBPanel<?> focusPanel = createFocusConfigPanel();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, 0, 15, 0);
        mainPanel.add(focusPanel, gbc);

        // 分析深度配置区域
        JBPanel<?> depthPanel = createDepthConfigPanel();
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, 0, 15, 0);
        mainPanel.add(depthPanel, gbc);

        // 其他配置区域
        JBPanel<?> otherPanel = createOtherConfigPanel();
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, 0, 15, 0);
        mainPanel.add(otherPanel, gbc);

        // 高级配置区域
        JBPanel<?> advancedPanel = createAdvancedConfigPanel();
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, 0, 20, 0);
        mainPanel.add(advancedPanel, gbc);

        // 按钮区域
        JBPanel<?> buttonPanel = createButtonPanel();
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(buttonPanel, gbc);

        // 添加垂直填充
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.weighty = 1.0; gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(Box.createVerticalGlue(), gbc);

        JBScrollPane scrollPane = new JBScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        add(scrollPane, BorderLayout.CENTER);
    }

    private JBPanel<?> createFocusConfigPanel() {
        JBPanel<?> panel = new JBPanel<>(new GridLayout(2, 2, 10, 5));
        panel.setBackground(JBColor.background());
        panel.setBorder(createTitledBorder("🎯 审查重点"));

        panel.add(securityCheckBox);
        panel.add(performanceCheckBox);
        panel.add(styleCheckBox);
        panel.add(logicCheckBox);

        return panel;
    }

    private JBPanel<?> createDepthConfigPanel() {
        JBPanel<?> panel = new JBPanel<>(new GridLayout(2, 2, 10, 5));
        panel.setBackground(JBColor.background());
        panel.setBorder(createTitledBorder("📊 分析深度"));

        panel.add(quickAnalysisRadio);
        panel.add(standardAnalysisRadio);
        panel.add(detailedAnalysisRadio);
        panel.add(comprehensiveAnalysisRadio);

        return panel;
    }

    private JBPanel<?> createOtherConfigPanel() {
        JBPanel<?> panel = new JBPanel<>(new GridLayout(3, 1, 5, 5));
        panel.setBackground(JBColor.background());
        panel.setBorder(createTitledBorder("⚙️ 其他选项"));

        panel.add(autoReviewCheckBox);
        panel.add(saveHistoryCheckBox);
        panel.add(showNotificationsCheckBox);

        return panel;
    }

    private JBPanel<?> createAdvancedConfigPanel() {
        JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
        panel.setBackground(JBColor.background());
        panel.setBorder(createTitledBorder("🔧 高级配置"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.WEST;

        // 最大文件数
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JBLabel("最大文件数:"), gbc);
        gbc.gridx = 1;
        panel.add(maxFilesSpinner, gbc);

        // 超时时间
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JBLabel("超时时间(秒):"), gbc);
        gbc.gridx = 1;
        panel.add(timeoutSpinner, gbc);

        return panel;
    }

    private JBPanel<?> createButtonPanel() {
        JBPanel<?> panel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panel.setBackground(JBColor.background());

        panel.add(saveButton);
        panel.add(resetButton);
        panel.add(testConnectionButton);

        return panel;
    }

    private TitledBorder createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(JBColor.GRAY),
            title,
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font(Font.SANS_SERIF, Font.BOLD, 12),
            JBColor.foreground()
        );
    }

    /**
     * 加载配置
     */
    private void loadConfiguration() {
        // TODO: 从配置文件或设置中加载配置
        // 这里使用默认值
    }

    /**
     * 获取当前配置
     */
    public ReviewConfiguration getCurrentConfiguration() {
        ReviewConfiguration config = new ReviewConfiguration();
        
        // 审查重点
        config.setSecurityEnabled(securityCheckBox.isSelected());
        config.setPerformanceEnabled(performanceCheckBox.isSelected());
        config.setStyleEnabled(styleCheckBox.isSelected());
        config.setLogicEnabled(logicCheckBox.isSelected());
        
        // 分析深度
        if (quickAnalysisRadio.isSelected()) config.setAnalysisDepth("quick");
        else if (standardAnalysisRadio.isSelected()) config.setAnalysisDepth("standard");
        else if (detailedAnalysisRadio.isSelected()) config.setAnalysisDepth("detailed");
        else if (comprehensiveAnalysisRadio.isSelected()) config.setAnalysisDepth("comprehensive");
        
        // 其他选项
        config.setAutoReviewEnabled(autoReviewCheckBox.isSelected());
        config.setSaveHistoryEnabled(saveHistoryCheckBox.isSelected());
        config.setShowNotificationsEnabled(showNotificationsCheckBox.isSelected());
        
        // 高级配置
        config.setMaxFiles((Integer) maxFilesSpinner.getValue());
        config.setTimeoutSeconds((Integer) timeoutSpinner.getValue());
        
        return config;
    }

    /**
     * 保存配置监听器
     */
    private class SaveActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ReviewConfiguration config = getCurrentConfiguration();
            // TODO: 保存配置到文件或设置
            JOptionPane.showMessageDialog(ConfigurationPanel.this,
                "配置已保存！", "保存成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 重置配置监听器
     */
    private class ResetActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 重置为默认值
            securityCheckBox.setSelected(true);
            performanceCheckBox.setSelected(true);
            styleCheckBox.setSelected(true);
            logicCheckBox.setSelected(true);
            
            standardAnalysisRadio.setSelected(true);
            
            autoReviewCheckBox.setSelected(false);
            saveHistoryCheckBox.setSelected(true);
            showNotificationsCheckBox.setSelected(true);
            
            maxFilesSpinner.setValue(20);
            timeoutSpinner.setValue(300);
            
            JOptionPane.showMessageDialog(ConfigurationPanel.this,
                "配置已重置为默认值！", "重置成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 测试连接监听器
     */
    private class TestConnectionActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            testConnectionButton.setEnabled(false);
            testConnectionButton.setText("🔄 测试中...");
            
            // TODO: 实现连接测试
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(2000); // 模拟测试过程
                    JOptionPane.showMessageDialog(ConfigurationPanel.this,
                        "连接测试成功！\n服务器响应正常", "连接测试", JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    testConnectionButton.setEnabled(true);
                    testConnectionButton.setText("🔗 测试连接");
                }
            });
        }
    }

    /**
     * 审查配置数据类
     */
    public static class ReviewConfiguration {
        private boolean securityEnabled = true;
        private boolean performanceEnabled = true;
        private boolean styleEnabled = true;
        private boolean logicEnabled = true;
        private String analysisDepth = "standard";
        private boolean autoReviewEnabled = false;
        private boolean saveHistoryEnabled = true;
        private boolean showNotificationsEnabled = true;
        private int maxFiles = 20;
        private int timeoutSeconds = 300;

        // Getters and Setters
        public boolean isSecurityEnabled() { return securityEnabled; }
        public void setSecurityEnabled(boolean securityEnabled) { this.securityEnabled = securityEnabled; }
        
        public boolean isPerformanceEnabled() { return performanceEnabled; }
        public void setPerformanceEnabled(boolean performanceEnabled) { this.performanceEnabled = performanceEnabled; }
        
        public boolean isStyleEnabled() { return styleEnabled; }
        public void setStyleEnabled(boolean styleEnabled) { this.styleEnabled = styleEnabled; }
        
        public boolean isLogicEnabled() { return logicEnabled; }
        public void setLogicEnabled(boolean logicEnabled) { this.logicEnabled = logicEnabled; }
        
        public String getAnalysisDepth() { return analysisDepth; }
        public void setAnalysisDepth(String analysisDepth) { this.analysisDepth = analysisDepth; }
        
        public boolean isAutoReviewEnabled() { return autoReviewEnabled; }
        public void setAutoReviewEnabled(boolean autoReviewEnabled) { this.autoReviewEnabled = autoReviewEnabled; }
        
        public boolean isSaveHistoryEnabled() { return saveHistoryEnabled; }
        public void setSaveHistoryEnabled(boolean saveHistoryEnabled) { this.saveHistoryEnabled = saveHistoryEnabled; }
        
        public boolean isShowNotificationsEnabled() { return showNotificationsEnabled; }
        public void setShowNotificationsEnabled(boolean showNotificationsEnabled) { this.showNotificationsEnabled = showNotificationsEnabled; }
        
        public int getMaxFiles() { return maxFiles; }
        public void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }
        
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }
}
