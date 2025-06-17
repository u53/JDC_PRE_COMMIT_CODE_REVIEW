package com.jdc.tools.precommit.ui.panels;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import com.jdc.tools.precommit.service.ReviewHistoryService;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 审查历史面板
 * 显示历史审查记录
 */
public class HistoryPanel extends JBPanel<HistoryPanel> {

    private final Project project;
    private JBPanel<?> historyListPanel;
    private JBLabel statusLabel;
    private final ReviewHistoryService historyService;

    public HistoryPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.historyService = ReviewHistoryService.getInstance();
        initializeUI();
        loadHistory();
    }

    /**
     * 初始化UI
     */
    private void initializeUI() {
        setBackground(JBColor.background());
        setBorder(JBUI.Borders.empty(10));

        // 标题栏
        JBPanel<?> titlePanel = new JBPanel<>(new BorderLayout());
        titlePanel.setBackground(JBColor.background());

        JBLabel titleLabel = new JBLabel("📋 审查历史");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titlePanel.add(titleLabel, BorderLayout.WEST);

        // 按钮面板
        JBPanel<?> buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(JBColor.background());

        JButton refreshButton = new JButton("🔄 刷新");
        refreshButton.setFont(refreshButton.getFont().deriveFont(12f));
        refreshButton.addActionListener(e -> refreshHistory());
        buttonPanel.add(refreshButton);

        JButton clearButton = new JButton("🗑️ 清空");
        clearButton.setFont(clearButton.getFont().deriveFont(12f));
        clearButton.addActionListener(e -> clearHistory());
        buttonPanel.add(clearButton);

        titlePanel.add(buttonPanel, BorderLayout.EAST);
        add(titlePanel, BorderLayout.NORTH);

        // 历史记录列表
        historyListPanel = new JBPanel<>();
        historyListPanel.setLayout(new BoxLayout(historyListPanel, BoxLayout.Y_AXIS));
        historyListPanel.setBackground(JBColor.background());

        JBScrollPane scrollPane = new JBScrollPane(historyListPanel);
        scrollPane.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // 状态标签
        statusLabel = new JBLabel("正在加载历史记录...");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        statusLabel.setForeground(JBColor.GRAY);
        statusLabel.setBorder(JBUI.Borders.empty(5, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * 加载历史记录
     */
    private void loadHistory() {
        updateHistoryList();
    }

    /**
     * 更新历史记录列表
     */
    private void updateHistoryList() {
        historyListPanel.removeAll();

        List<ReviewHistoryService.ReviewHistoryRecord> historyRecords = historyService.getHistory();

        if (historyRecords.isEmpty()) {
            JBLabel emptyLabel = new JBLabel("暂无审查历史");
            emptyLabel.setForeground(JBColor.GRAY);
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setBorder(JBUI.Borders.empty(50));
            historyListPanel.add(emptyLabel);

            statusLabel.setText("📝 暂无审查历史记录");
            statusLabel.setForeground(JBColor.GRAY);
        } else {
            for (ReviewHistoryService.ReviewHistoryRecord record : historyRecords) {
                JBPanel<?> recordPanel = createHistoryRecordPanel(record);
                historyListPanel.add(recordPanel);
                historyListPanel.add(Box.createVerticalStrut(10));
            }

            statusLabel.setText("✅ 共 " + historyRecords.size() + " 条审查记录");
            statusLabel.setForeground(JBColor.GREEN);
        }

        historyListPanel.revalidate();
        historyListPanel.repaint();
    }

    /**
     * 创建历史记录面板
     */
    private JBPanel<?> createHistoryRecordPanel(ReviewHistoryService.ReviewHistoryRecord record) {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBackground(JBColor.background());
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.border(), 1),
            JBUI.Borders.empty(12)
        ));

        // 顶部信息
        JBPanel<?> topPanel = new JBPanel<>(new BorderLayout());
        topPanel.setBackground(JBColor.background());

        // 时间和ID
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        JBLabel timeLabel = new JBLabel("🕒 " + sdf.format(record.getTimestamp()));
        timeLabel.setFont(timeLabel.getFont().deriveFont(Font.BOLD, 12f));
        topPanel.add(timeLabel, BorderLayout.WEST);

        JBLabel idLabel = new JBLabel("ID: " + record.getReviewId());
        idLabel.setFont(idLabel.getFont().deriveFont(Font.ITALIC, 10f));
        idLabel.setForeground(JBColor.GRAY);
        topPanel.add(idLabel, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);

        // 中间信息
        JBPanel<?> middlePanel = new JBPanel<>(new GridLayout(3, 2, 10, 5));
        middlePanel.setBackground(JBColor.background());
        middlePanel.setBorder(JBUI.Borders.empty(8, 0, 8, 0));

        // 项目名称
        JBLabel projectLabel = new JBLabel("📁 项目: " + (record.getProjectName() != null ? record.getProjectName() : "未知"));
        projectLabel.setFont(projectLabel.getFont().deriveFont(11f));
        middlePanel.add(projectLabel);

        // 文件数量
        JBLabel filesLabel = new JBLabel("📄 文件数: " + record.getFileCount());
        filesLabel.setFont(filesLabel.getFont().deriveFont(11f));
        middlePanel.add(filesLabel);

        // 质量评分
        String scoreText = record.getQualityScore() != null ? record.getQualityScore() + "/100" : "未评分";
        JBLabel scoreLabel = new JBLabel("⭐ 评分: " + scoreText);
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 11f));
        if (record.getQualityScore() != null) {
            scoreLabel.setForeground(getScoreColor(record.getQualityScore()));
        }
        middlePanel.add(scoreLabel);

        // 建议数量
        JBLabel suggestionsLabel = new JBLabel("💡 建议: " + record.getSuggestionCount() + "条");
        suggestionsLabel.setFont(suggestionsLabel.getFont().deriveFont(11f));
        middlePanel.add(suggestionsLabel);

        // 处理时间
        String timeText = record.getProcessingTime() != null ? formatTime(record.getProcessingTime()) : "未知";
        JBLabel processingTimeLabel = new JBLabel("⏱️ 耗时: " + timeText);
        processingTimeLabel.setFont(processingTimeLabel.getFont().deriveFont(11f));
        middlePanel.add(processingTimeLabel);

        // 空白占位
        middlePanel.add(new JBLabel(""));

        panel.add(middlePanel, BorderLayout.CENTER);

        // 底部摘要
        if (record.getSummary() != null && !record.getSummary().trim().isEmpty()) {
            JTextArea summaryArea = new JTextArea(record.getSummary());
            summaryArea.setEditable(false);
            summaryArea.setLineWrap(true);
            summaryArea.setWrapStyleWord(true);
            summaryArea.setBackground(JBColor.background());
            summaryArea.setForeground(JBColor.GRAY);
            summaryArea.setFont(summaryArea.getFont().deriveFont(Font.ITALIC, 10f));
            summaryArea.setBorder(JBUI.Borders.empty(5, 0, 0, 0));
            panel.add(summaryArea, BorderLayout.SOUTH);
        }

        return panel;
    }

    /**
     * 获取评分颜色
     */
    private Color getScoreColor(int score) {
        if (score >= 90) return JBColor.GREEN;
        if (score >= 80) return JBColor.BLUE;
        if (score >= 70) return JBColor.ORANGE;
        if (score >= 60) return JBColor.YELLOW;
        return JBColor.RED;
    }

    /**
     * 刷新历史记录
     */
    public void refreshHistory() {
        statusLabel.setText("正在刷新历史记录...");
        statusLabel.setForeground(JBColor.GRAY);

        // 模拟异步加载
        SwingUtilities.invokeLater(() -> {
            loadHistory();
        });
    }

    /**
     * 清空历史记录
     */
    public void clearHistory() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "确定要清空所有审查历史记录吗？",
            "确认清空",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            historyService.clearHistory();
            updateHistoryList();
        }
    }

    /**
     * 格式化时间
     */
    private String formatTime(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else {
            return String.format("%.1fs", milliseconds / 1000.0);
        }
    }

}
