package com.jdc.tools.precommit.ui.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * 可折叠的建议面板
 * 提供展开/折叠功能的建议显示组件
 */
public class CollapsibleSuggestionPanel extends JBPanel<CollapsibleSuggestionPanel> {

    private final int index;
    private final String suggestion;
    private final Map<String, Object> structuredData;
    private boolean isExpanded = false;

    private JBPanel<?> headerPanel;
    private JBPanel<?> contentPanel;
    private JBLabel expandIcon;
    private JBLabel titleLabel;
    private JBLabel summaryLabel;
    private JTextArea contentArea;

    public CollapsibleSuggestionPanel(int index, String suggestion) {
        this(index, suggestion, null);
    }

    public CollapsibleSuggestionPanel(int index, String suggestion, Map<String, Object> structuredData) {
        super(new BorderLayout());
        this.index = index;
        this.suggestion = suggestion;
        this.structuredData = structuredData;

        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        setBackground(JBColor.background());
        setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(getSuggestionBorderColor(), 1),
            JBUI.Borders.empty(2)
        ));

        // 头部面板
        headerPanel = new JBPanel<>(new BorderLayout());
        headerPanel.setBackground(getSuggestionHeaderColor());
        headerPanel.setBorder(JBUI.Borders.empty(8, 12));
        headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 展开/折叠图标
        expandIcon = new JBLabel("▶");
        expandIcon.setFont(expandIcon.getFont().deriveFont(Font.BOLD, 12f));
        expandIcon.setForeground(JBColor.GRAY);

        // 标题标签
        String title = getDisplayTitle();
        titleLabel = new JBLabel(String.format("#%d %s", index, title));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        titleLabel.setForeground(getSuggestionPriorityColor()); // 使用优先级颜色

        // 摘要标签（折叠时显示）
        String summary = getDisplaySummary();
        summaryLabel = new JBLabel(summary);
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(12f));
        summaryLabel.setForeground(JBColor.GRAY);

        // 内容面板
        contentPanel = new JBPanel<>(new BorderLayout());
        contentPanel.setBackground(JBColor.background());
        contentPanel.setBorder(JBUI.Borders.empty(0, 12, 12, 12));
        contentPanel.setVisible(false);

        // 内容文本区域 - 修复颜色问题
        contentArea = new JTextArea(getDisplayDescription());
        contentArea.setEditable(false);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setBackground(JBColor.background());
        contentArea.setForeground(JBColor.foreground()); // 确保使用正确的前景色
        contentArea.setFont(contentArea.getFont().deriveFont(12f));
        contentArea.setBorder(JBUI.Borders.empty(8));
        contentArea.setCaretColor(JBColor.foreground()); // 设置光标颜色
    }

    private void setupLayout() {
        // 头部布局
        JBPanel<?> headerContent = new JBPanel<>(new BorderLayout());
        headerContent.setBackground(getSuggestionHeaderColor());

        // 左侧：图标和标题
        JBPanel<?> leftPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setBackground(getSuggestionHeaderColor());
        leftPanel.add(expandIcon);
        leftPanel.add(Box.createHorizontalStrut(8));
        leftPanel.add(titleLabel);

        // 右侧：优先级标签
        JBPanel<?> rightPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setBackground(getSuggestionHeaderColor());

        JBLabel priorityLabel = new JBLabel(getSuggestionPriority());
        priorityLabel.setFont(priorityLabel.getFont().deriveFont(Font.BOLD, 10f));
        priorityLabel.setForeground(getSuggestionPriorityColor());
        priorityLabel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(getSuggestionPriorityColor(), 1),
            JBUI.Borders.empty(2, 6)
        ));
        rightPanel.add(priorityLabel);

        headerContent.add(leftPanel, BorderLayout.WEST);
        headerContent.add(rightPanel, BorderLayout.EAST);

        // 摘要面板（折叠时显示）
        JBPanel<?> summaryPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 32, 0));
        summaryPanel.setBackground(getSuggestionHeaderColor());
        summaryPanel.add(summaryLabel);

        // 完整的头部面板
        JBPanel<?> fullHeaderPanel = new JBPanel<>(new BorderLayout());
        fullHeaderPanel.setBackground(getSuggestionHeaderColor());
        fullHeaderPanel.add(headerContent, BorderLayout.NORTH);
        fullHeaderPanel.add(summaryPanel, BorderLayout.CENTER);

        headerPanel.add(fullHeaderPanel, BorderLayout.CENTER);

        // 内容布局
        contentPanel.add(contentArea, BorderLayout.CENTER);

        // 主面板布局
        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        // 头部点击事件
        MouseAdapter clickHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleExpansion();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                headerPanel.setBackground(getSuggestionHoverColor());
                headerPanel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                headerPanel.setBackground(getSuggestionHeaderColor());
                headerPanel.repaint();
            }
        };

        headerPanel.addMouseListener(clickHandler);
        expandIcon.addMouseListener(clickHandler);
        titleLabel.addMouseListener(clickHandler);
    }

    /**
     * 切换展开/折叠状态
     */
    private void toggleExpansion() {
        isExpanded = !isExpanded;

        // 更新图标
        expandIcon.setText(isExpanded ? "▼" : "▶");

        // 更新内容面板可见性
        contentPanel.setVisible(isExpanded);

        // 重新布局
        revalidate();
        repaint();

        // 通知父容器重新布局
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    /**
     * 获取显示标题
     */
    private String getDisplayTitle() {
        if (structuredData != null) {
            String title = (String) structuredData.get("title");
            if (title != null && !title.trim().isEmpty()) {
                return title;
            }
        }
        return extractSuggestionTitle(suggestion);
    }

    /**
     * 获取显示摘要
     */
    private String getDisplaySummary() {
        if (structuredData != null) {
            String summary = (String) structuredData.get("summary");
            if (summary != null && !summary.trim().isEmpty()) {
                return summary;
            }
        }
        return extractSuggestionSummary(suggestion);
    }

    /**
     * 获取显示描述
     */
    private String getDisplayDescription() {
        if (structuredData != null) {
            String description = (String) structuredData.get("description");
            if (description != null && !description.trim().isEmpty()) {
                return description;
            }
        }
        return suggestion;
    }

    /**
     * 提取建议标题（取第一行或前50个字符）
     */
    private String extractSuggestionTitle(String suggestion) {
        if (suggestion == null || suggestion.trim().isEmpty()) {
            return "改进建议";
        }

        String[] lines = suggestion.split("\n");
        String firstLine = lines[0].trim();

        if (firstLine.length() > 50) {
            return firstLine.substring(0, 47) + "...";
        }

        return firstLine.isEmpty() ? "改进建议" : firstLine;
    }

    /**
     * 提取建议摘要
     */
    private String extractSuggestionSummary(String suggestion) {
        if (suggestion == null || suggestion.trim().isEmpty()) {
            return "点击查看详细内容";
        }

        String cleaned = suggestion.replaceAll("\n", " ").trim();
        if (cleaned.length() > 80) {
            return cleaned.substring(0, 77) + "...";
        }

        return cleaned;
    }

    /**
     * 获取建议优先级
     */
    private String getSuggestionPriority() {
        if (structuredData != null) {
            String priority = (String) structuredData.get("priority");
            if (priority != null) {
                switch (priority.toLowerCase()) {
                    case "high": return "高优先级";
                    case "medium": return "中优先级";
                    case "low": return "低优先级";
                    default: return "中优先级";
                }
            }
        }

        // 降级到关键词分析
        String lowerSuggestion = suggestion.toLowerCase();

        if (lowerSuggestion.contains("安全") || lowerSuggestion.contains("漏洞") ||
            lowerSuggestion.contains("security") || lowerSuggestion.contains("vulnerability")) {
            return "高优先级";
        } else if (lowerSuggestion.contains("性能") || lowerSuggestion.contains("优化") ||
                   lowerSuggestion.contains("performance") || lowerSuggestion.contains("optimize")) {
            return "中优先级";
        } else {
            return "低优先级";
        }
    }

    /**
     * 获取建议边框颜色
     */
    private Color getSuggestionBorderColor() {
        String priority = getSuggestionPriority();
        switch (priority) {
            case "高优先级": return new Color(220, 53, 69);   // 红色
            case "中优先级": return new Color(255, 140, 0);   // 橙色
            default: return new Color(108, 117, 125);         // 灰色
        }
    }

    /**
     * 获取建议头部背景色
     */
    private Color getSuggestionHeaderColor() {
        String priority = getSuggestionPriority();
        switch (priority) {
            case "高优先级": return new Color(248, 215, 218); // 浅红色
            case "中优先级": return new Color(255, 243, 205); // 浅橙色
            default: return new Color(233, 236, 239);         // 浅灰色
        }
    }

    /**
     * 获取建议悬停背景色
     */
    private Color getSuggestionHoverColor() {
        String priority = getSuggestionPriority();
        switch (priority) {
            case "高优先级": return new Color(242, 222, 222); // 更浅的红色
            case "中优先级": return new Color(255, 248, 220); // 更浅的橙色
            default: return new Color(248, 249, 250);         // 更浅的灰色
        }
    }

    /**
     * 获取优先级标签颜色
     */
    private Color getSuggestionPriorityColor() {
        String priority = getSuggestionPriority();
        switch (priority) {
            case "高优先级": return new Color(220, 53, 69);   // 红色
            case "中优先级": return new Color(255, 140, 0);   // 橙色
            default: return new Color(108, 117, 125);         // 灰色
        }
    }

    /**
     * 程序化展开面板
     */
    public void expand() {
        if (!isExpanded) {
            toggleExpansion();
        }
    }

    /**
     * 程序化折叠面板
     */
    public void collapse() {
        if (isExpanded) {
            toggleExpansion();
        }
    }

    /**
     * 获取展开状态
     */
    public boolean isExpanded() {
        return isExpanded;
    }
}
