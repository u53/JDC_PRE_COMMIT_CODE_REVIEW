package com.jdc.tools.precommit.ui.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import com.jdc.tools.precommit.model.PreCommitReviewResponse;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

/**
 * 增强的结果展示面板
 * 提供更美观和功能丰富的审查结果显示
 */
public class EnhancedResultPanel extends JBPanel<EnhancedResultPanel> {

    private JBLabel qualityScoreLabel;
    private JProgressBar qualityProgressBar;
    private JTextArea analysisTextArea;
    private JBPanel<?> suggestionsContainer;
    private JBPanel<?> complexityPanel;
    private JBLabel processingTimeLabel;
    private JButton exportButton;
    private JButton copyButton;

    public EnhancedResultPanel() {
        super(new BorderLayout());
        initializeComponents();
        setupLayout();
        showWelcomeMessage();
    }

    private void initializeComponents() {
        setBackground(JBColor.background());
        setBorder(JBUI.Borders.empty(10));

        // 质量评分组件
        qualityScoreLabel = new JBLabel("质量评分: --/100");
        qualityScoreLabel.setFont(qualityScoreLabel.getFont().deriveFont(Font.BOLD, 16f));

        qualityProgressBar = new JProgressBar(0, 100);
        qualityProgressBar.setStringPainted(true);
        qualityProgressBar.setPreferredSize(new Dimension(200, 25));

        // 分析结果文本区域
        analysisTextArea = new JTextArea();
        analysisTextArea.setEditable(false);
        analysisTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        analysisTextArea.setBackground(JBColor.background());
        analysisTextArea.setForeground(JBColor.foreground());
        analysisTextArea.setLineWrap(true);
        analysisTextArea.setWrapStyleWord(true);

        // 建议容器
        suggestionsContainer = new JBPanel<>();
        suggestionsContainer.setLayout(new BoxLayout(suggestionsContainer, BoxLayout.Y_AXIS));
        suggestionsContainer.setBackground(JBColor.background());

        // 复杂度面板
        complexityPanel = new JBPanel<>(new GridLayout(2, 2, 10, 5));
        complexityPanel.setBackground(JBColor.background());
        complexityPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(JBColor.GRAY),
            "代码复杂度分析",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font(Font.SANS_SERIF, Font.BOLD, 12),
            JBColor.foreground()
        ));

        // 处理时间标签
        processingTimeLabel = new JBLabel("处理时间: --");
        processingTimeLabel.setFont(processingTimeLabel.getFont().deriveFont(Font.ITALIC, 11f));
        processingTimeLabel.setForeground(JBColor.GRAY);

        // 操作按钮
        exportButton = new JButton("📄 导出报告");
        exportButton.setToolTipText("导出审查报告为HTML文件");
        exportButton.addActionListener(new ExportActionListener());

        copyButton = new JButton("📋 复制结果");
        copyButton.setToolTipText("复制审查结果到剪贴板");
        copyButton.addActionListener(new CopyActionListener());
    }

    private void setupLayout() {
        // 顶部面板 - 质量评分和操作按钮
        JBPanel<?> topPanel = new JBPanel<>(new BorderLayout());
        topPanel.setBackground(JBColor.background());
        topPanel.setBorder(JBUI.Borders.empty(0, 0, 15, 0));

        // 质量评分区域
        JBPanel<?> scorePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        scorePanel.setBackground(JBColor.background());
        scorePanel.add(qualityScoreLabel);
        scorePanel.add(Box.createHorizontalStrut(15));
        scorePanel.add(qualityProgressBar);
        topPanel.add(scorePanel, BorderLayout.WEST);

        // 操作按钮区域
        JBPanel<?> buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(JBColor.background());
        buttonPanel.add(copyButton);
        buttonPanel.add(exportButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 中心内容区域 - 使用标签页
        JBTabbedPane tabbedPane = new JBTabbedPane();

        // 分析结果标签页
        JBScrollPane analysisScrollPane = new JBScrollPane(analysisTextArea);
        analysisScrollPane.setBorder(JBUI.Borders.empty(10));
        analysisScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tabbedPane.addTab("📊 详细分析", analysisScrollPane);

        // 改进建议标签页
        JBScrollPane suggestionsScrollPane = new JBScrollPane(suggestionsContainer);
        suggestionsScrollPane.setBorder(JBUI.Borders.empty(10));
        suggestionsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tabbedPane.addTab("💡 改进建议", suggestionsScrollPane);

        // 复杂度分析标签页
        JBScrollPane complexityScrollPane = new JBScrollPane(complexityPanel);
        complexityScrollPane.setBorder(JBUI.Borders.empty(10));
        tabbedPane.addTab("📈 复杂度分析", complexityScrollPane);

        add(tabbedPane, BorderLayout.CENTER);

        // 底部状态栏
        JBPanel<?> statusPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBackground(JBColor.background());
        statusPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        statusPanel.add(processingTimeLabel);
        add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * 显示欢迎消息
     */
    private void showWelcomeMessage() {
        analysisTextArea.setText(
            "🎯 JDC Git提交前代码审查\n\n" +
            "✨ 功能特性:\n" +
            "• AI驱动的智能代码分析\n" +
            "• 多维度质量评估 (安全性、性能、风格、逻辑)\n" +
            "• 详细的改进建议和最佳实践指导\n" +
            "• 代码复杂度分析和可视化\n" +
            "• 支持多种编程语言\n\n" +
            "🚀 使用步骤:\n" +
            "1. 使用 git add 添加文件到暂存区\n" +
            "2. 点击 \"🔍 开始审查\" 按钮\n" +
            "3. 等待AI分析完成\n" +
            "4. 查看详细的审查结果和建议\n\n" +
            "💡 提示: 建议在每次提交前进行代码审查，以确保代码质量"
        );

        // 禁用操作按钮
        exportButton.setEnabled(false);
        copyButton.setEnabled(false);
    }

    /**
     * 显示审查结果
     */
    public void displayResult(PreCommitReviewResponse response) {
        // 更新质量评分
        updateQualityScore(response.getQualityScore());

        // 更新分析结果
        if (response.getAnalysis() != null && !response.getAnalysis().trim().isEmpty()) {
            analysisTextArea.setText(response.getAnalysis());
            analysisTextArea.setCaretPosition(0);
        }

        // 更新改进建议 - 优先使用结构化数据
        if (response.getStructuredSuggestions() != null && !response.getStructuredSuggestions().isEmpty()) {
            updateStructuredSuggestions(response.getStructuredSuggestions());
        } else {
            updateSuggestions(response.getSuggestions());
        }

        // 更新复杂度分析
        updateComplexityAnalysis(response.getComplexity());

        // 更新处理时间
        if (response.getProcessingTime() != null) {
            String timeText = formatProcessingTime(response.getProcessingTime());
            processingTimeLabel.setText("处理时间: " + timeText);
        }

        // 启用操作按钮
        exportButton.setEnabled(true);
        copyButton.setEnabled(true);
    }

    /**
     * 更新质量评分显示
     */
    private void updateQualityScore(Integer score) {
        if (score != null) {
            qualityScoreLabel.setText("质量评分: " + score + "/100");
            qualityProgressBar.setValue(score);
            qualityProgressBar.setString(score + "/100");

            // 根据评分设置颜色
            Color scoreColor = getScoreColor(score);
            qualityScoreLabel.setForeground(scoreColor);
            qualityProgressBar.setForeground(scoreColor);
        }
    }

    /**
     * 更新结构化改进建议
     */
    private void updateStructuredSuggestions(List<Map<String, Object>> structuredSuggestions) {
        suggestionsContainer.removeAll();

        if (structuredSuggestions == null || structuredSuggestions.isEmpty()) {
            JBLabel noSuggestionsLabel = new JBLabel("🎉 太棒了！暂无改进建议，代码质量很好！");
            noSuggestionsLabel.setForeground(JBColor.GREEN);
            noSuggestionsLabel.setFont(noSuggestionsLabel.getFont().deriveFont(Font.BOLD, 14f));
            noSuggestionsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noSuggestionsLabel.setBorder(JBUI.Borders.empty(30));
            suggestionsContainer.add(noSuggestionsLabel);
        } else {
            for (int i = 0; i < structuredSuggestions.size(); i++) {
                Map<String, Object> suggestionData = structuredSuggestions.get(i);
                String description = (String) suggestionData.get("description");
                if (description == null) {
                    description = (String) suggestionData.get("title");
                }
                if (description == null) {
                    description = "改进建议";
                }

                CollapsibleSuggestionPanel suggestionPanel = new CollapsibleSuggestionPanel(
                    i + 1, description, suggestionData
                );
                suggestionsContainer.add(suggestionPanel);
                suggestionsContainer.add(Box.createVerticalStrut(8));
            }
        }

        suggestionsContainer.revalidate();
        suggestionsContainer.repaint();
    }

    /**
     * 更新改进建议（降级方法）
     */
    private void updateSuggestions(List<String> suggestions) {
        suggestionsContainer.removeAll();

        if (suggestions == null || suggestions.isEmpty()) {
            JBLabel noSuggestionsLabel = new JBLabel("🎉 太棒了！暂无改进建议，代码质量很好！");
            noSuggestionsLabel.setForeground(JBColor.GREEN);
            noSuggestionsLabel.setFont(noSuggestionsLabel.getFont().deriveFont(Font.BOLD, 14f));
            noSuggestionsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noSuggestionsLabel.setBorder(JBUI.Borders.empty(30));
            suggestionsContainer.add(noSuggestionsLabel);
        } else {
            for (int i = 0; i < suggestions.size(); i++) {
                CollapsibleSuggestionPanel suggestionPanel = new CollapsibleSuggestionPanel(
                    i + 1, suggestions.get(i)
                );
                suggestionsContainer.add(suggestionPanel);
                suggestionsContainer.add(Box.createVerticalStrut(8));
            }
        }

        suggestionsContainer.revalidate();
        suggestionsContainer.repaint();
    }

    /**
     * 更新复杂度分析
     */
    private void updateComplexityAnalysis(Object complexity) {
        complexityPanel.removeAll();

        if (complexity != null) {
            // 这里可以根据实际的复杂度数据结构来显示
            // 暂时显示一些示例指标
            addComplexityMetric("圈复杂度", "中等", JBColor.ORANGE);
            addComplexityMetric("认知复杂度", "低", JBColor.GREEN);
            addComplexityMetric("代码重复度", "低", JBColor.GREEN);
            addComplexityMetric("测试覆盖度", "待提升", JBColor.YELLOW);
        } else {
            JBLabel noDataLabel = new JBLabel("暂无复杂度数据");
            noDataLabel.setForeground(JBColor.GRAY);
            noDataLabel.setHorizontalAlignment(SwingConstants.CENTER);
            complexityPanel.add(noDataLabel);
        }

        complexityPanel.revalidate();
        complexityPanel.repaint();
    }

    /**
     * 添加复杂度指标
     */
    private void addComplexityMetric(String name, String value, Color color) {
        JBLabel nameLabel = new JBLabel(name + ":");
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));

        JBLabel valueLabel = new JBLabel(value);
        valueLabel.setForeground(color);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 12f));

        complexityPanel.add(nameLabel);
        complexityPanel.add(valueLabel);
    }

    /**
     * 格式化处理时间
     */
    private String formatProcessingTime(Long timeMs) {
        if (timeMs < 1000) {
            return timeMs + "ms";
        } else {
            return String.format("%.1fs", timeMs / 1000.0);
        }
    }

    /**
     * 获取评分对应的颜色
     */
    private Color getScoreColor(int score) {
        if (score >= 90) return new Color(34, 139, 34);  // 深绿色
        if (score >= 80) return new Color(0, 123, 255);   // 蓝色
        if (score >= 70) return new Color(255, 140, 0);   // 橙色
        if (score >= 60) return new Color(255, 193, 7);   // 黄色
        return new Color(220, 53, 69);                     // 红色
    }

    /**
     * 清除结果显示
     */
    public void clearResults() {
        qualityScoreLabel.setText("质量评分: --/100");
        qualityProgressBar.setValue(0);
        qualityProgressBar.setString("分析中...");

        analysisTextArea.setText("🔄 正在进行AI代码审查...\n\n" +
            "⏳ 请稍候，AI正在深度分析您的代码变动\n" +
            "📊 分析维度包括：安全性、性能、代码风格、逻辑结构\n" +
            "💡 即将为您提供专业的改进建议");

        suggestionsContainer.removeAll();
        JBLabel loadingLabel = new JBLabel("⏳ 正在生成改进建议...");
        loadingLabel.setForeground(JBColor.GRAY);
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setBorder(JBUI.Borders.empty(30));
        suggestionsContainer.add(loadingLabel);

        complexityPanel.removeAll();
        JBLabel complexityLoadingLabel = new JBLabel("📈 正在分析代码复杂度...");
        complexityLoadingLabel.setForeground(JBColor.GRAY);
        complexityLoadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        complexityPanel.add(complexityLoadingLabel);

        processingTimeLabel.setText("处理时间: 分析中...");

        // 禁用操作按钮
        exportButton.setEnabled(false);
        copyButton.setEnabled(false);

        revalidate();
        repaint();
    }

    /**
     * 导出操作监听器
     */
    private class ExportActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO: 实现导出功能
            JOptionPane.showMessageDialog(EnhancedResultPanel.this,
                "导出功能开发中...", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 复制操作监听器
     */
    private class CopyActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO: 实现复制功能
            JOptionPane.showMessageDialog(EnhancedResultPanel.this,
                "复制功能开发中...", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
