package com.jdc.tools.precommit.ui.panels;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import com.jdc.tools.precommit.exception.ReviewException;
import com.jdc.tools.precommit.model.PreCommitReviewResponse;
import com.jdc.tools.precommit.service.GitAnalysisService;
import com.jdc.tools.precommit.service.PreCommitReviewService;
import com.jdc.tools.precommit.service.ReviewHistoryService;
import com.jdc.tools.precommit.ui.components.EnhancedResultPanel;
import com.jdc.tools.precommit.ui.components.ErrorDisplayPanel;
import com.jdc.tools.precommit.ui.FileSelectionDialog;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 代码审查面板
 * 显示暂存文件和审查结果
 */
public class ReviewPanel extends JBPanel<ReviewPanel> {

    private final Project project;
    private JBPanel<?> stagedFilesPanel;
    private EnhancedResultPanel enhancedResultPanel;
    private ErrorDisplayPanel errorDisplayPanel;
    private JButton reviewButton;
    private JButton refreshButton;
    private JBLabel statusLabel;
    private JBTabbedPane mainTabbedPane;
    private final ReviewHistoryService historyService;
    private List<JCheckBox> fileCheckBoxes = new ArrayList<>();

    public ReviewPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.historyService = ReviewHistoryService.getInstance();
        initializeUI();
        refreshStagedFiles();
    }

    /**
     * 初始化UI
     */
    private void initializeUI() {
        setBackground(JBColor.background());
        setBorder(JBUI.Borders.empty(10));

        // 创建主标签页
        mainTabbedPane = new JBTabbedPane();

        // 创建水平分割面板 - 左侧暂存文件，右侧审查结果
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.3); // 左侧占30%，右侧占70%
        splitPane.setBorder(null);
        splitPane.setDividerSize(5);

        // 左侧：暂存文件区域
        JBPanel<?> leftPanel = createStagedFilesArea();
        leftPanel.setPreferredSize(new Dimension(350, 0)); // 设置最小宽度
        splitPane.setLeftComponent(leftPanel);

        // 右侧：创建结果显示区域
        JBPanel<?> rightPanel = createEnhancedResultArea();
        splitPane.setRightComponent(rightPanel);

        // 添加到主标签页
        mainTabbedPane.addTab("🔍 代码审查", splitPane);

        add(mainTabbedPane, BorderLayout.CENTER);
    }

    /**
     * 创建暂存文件区域
     */
    private JBPanel<?> createStagedFilesArea() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBackground(JBColor.background());
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.border(), 1),
            JBUI.Borders.empty(10)
        ));

        // 标题栏
        JBPanel<?> titlePanel = new JBPanel<>(new BorderLayout());
        titlePanel.setBackground(JBColor.background());

        JBLabel titleLabel = new JBLabel("📁 暂存区文件");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titlePanel.add(titleLabel, BorderLayout.WEST);

        // 按钮面板
        JBPanel<?> buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(JBColor.background());

        refreshButton = new JButton("🔄 刷新");
        refreshButton.setFont(refreshButton.getFont().deriveFont(12f));
        refreshButton.setToolTipText("刷新暂存区文件列表");
        refreshButton.addActionListener(e -> refreshStagedFiles());
        buttonPanel.add(refreshButton);

        // 全选/全不选按钮
        JButton selectAllButton = new JButton("全选");
        selectAllButton.setFont(selectAllButton.getFont().deriveFont(11f));
        selectAllButton.setToolTipText("选择所有文件");
        selectAllButton.addActionListener(e -> selectAllFiles(true));
        buttonPanel.add(selectAllButton);

        JButton deselectAllButton = new JButton("全不选");
        deselectAllButton.setFont(deselectAllButton.getFont().deriveFont(11f));
        deselectAllButton.setToolTipText("取消选择所有文件");
        deselectAllButton.addActionListener(e -> selectAllFiles(false));
        buttonPanel.add(deselectAllButton);

        reviewButton = new JButton("🎯 审查选中文件");
        reviewButton.setFont(reviewButton.getFont().deriveFont(Font.BOLD, 12f));
        reviewButton.setToolTipText("审查选中的文件");
        reviewButton.setBackground(JBColor.BLUE);
        reviewButton.setForeground(JBColor.WHITE);
        reviewButton.setOpaque(true);
        reviewButton.setBorderPainted(false);
        reviewButton.setFocusPainted(false);
        reviewButton.setPreferredSize(new Dimension(130, 30));
        reviewButton.addActionListener(new ReviewActionListener());
        buttonPanel.add(reviewButton);

        titlePanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(titlePanel, BorderLayout.NORTH);

        // 文件列表区域
        stagedFilesPanel = new JBPanel<>();
        stagedFilesPanel.setLayout(new BoxLayout(stagedFilesPanel, BoxLayout.Y_AXIS));
        stagedFilesPanel.setBackground(JBColor.background());

        JBScrollPane scrollPane = new JBScrollPane(stagedFilesPanel);
        scrollPane.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 状态标签
        statusLabel = new JBLabel("正在加载暂存区文件...");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        statusLabel.setForeground(JBColor.GRAY);
        statusLabel.setBorder(JBUI.Borders.empty(5, 0, 0, 0));
        panel.add(statusLabel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 创建增强的结果显示区域
     */
    private JBPanel<?> createEnhancedResultArea() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBackground(JBColor.background());
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.border(), 1),
            JBUI.Borders.empty(5)
        ));

        // 创建卡片式布局
        CardLayout cardLayout = new CardLayout();
        JBPanel<?> cardPanel = new JBPanel<>(cardLayout);
        cardPanel.setBackground(JBColor.background());

        // 增强结果面板
        enhancedResultPanel = new EnhancedResultPanel();
        cardPanel.add(enhancedResultPanel, "RESULT");

        // 错误显示面板
        errorDisplayPanel = new ErrorDisplayPanel();
        cardPanel.add(errorDisplayPanel, "ERROR");

        // 默认显示结果面板
        cardLayout.show(cardPanel, "RESULT");

        panel.add(cardPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 刷新暂存文件列表
     */
    public void refreshStagedFiles() {
        stagedFilesPanel.removeAll();
        statusLabel.setText("正在加载暂存区文件...");

        // 异步加载文件列表
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            GitAnalysisService gitService = GitAnalysisService.getInstance();
            List<GitAnalysisService.StagedFileInfo> stagedFiles = gitService.getStagedFiles(project);

            ApplicationManager.getApplication().invokeLater(() -> {
                updateStagedFilesList(stagedFiles);
            });
        });
    }

    /**
     * 更新暂存文件列表
     */
    private void updateStagedFilesList(List<GitAnalysisService.StagedFileInfo> stagedFiles) {
        stagedFilesPanel.removeAll();
        fileCheckBoxes.clear();

        if (stagedFiles.isEmpty()) {
            JBLabel emptyLabel = new JBLabel("暂存区为空");
            emptyLabel.setForeground(JBColor.GRAY);
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setBorder(JBUI.Borders.empty(20));
            stagedFilesPanel.add(emptyLabel);

            statusLabel.setText("⚠️ 暂存区没有文件，请使用 git add 添加文件");
            statusLabel.setForeground(JBColor.ORANGE);
            reviewButton.setEnabled(false);
        } else {
            for (GitAnalysisService.StagedFileInfo fileInfo : stagedFiles) {
                JBPanel<?> filePanel = createFileInfoPanelWithCheckbox(fileInfo);
                stagedFilesPanel.add(filePanel);
                stagedFilesPanel.add(Box.createVerticalStrut(5));
            }

            statusLabel.setText("✅ 找到 " + stagedFiles.size() + " 个暂存文件");
            statusLabel.setForeground(JBColor.GREEN);
            reviewButton.setEnabled(true);
        }

        stagedFilesPanel.revalidate();
        stagedFilesPanel.repaint();
    }

    /**
     * 创建带复选框的文件信息面板
     */
    private JBPanel<?> createFileInfoPanelWithCheckbox(GitAnalysisService.StagedFileInfo fileInfo) {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBackground(JBColor.background());
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.LIGHT_GRAY, 1),
            JBUI.Borders.empty(8)
        ));

        // 左侧复选框
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(true); // 默认选中
        checkBox.setBackground(JBColor.background());
        checkBox.putClientProperty("fileInfo", fileInfo); // 存储文件信息
        fileCheckBoxes.add(checkBox);
        panel.add(checkBox, BorderLayout.WEST);

        // 中间文件信息
        JBPanel<?> infoPanel = new JBPanel<>(new BorderLayout());
        infoPanel.setBackground(JBColor.background());
        infoPanel.setBorder(JBUI.Borders.empty(0, 10, 0, 0));

        // 文件名和状态
        JBPanel<?> topPanel = new JBPanel<>(new BorderLayout());
        topPanel.setBackground(JBColor.background());

        JBLabel fileNameLabel = new JBLabel(fileInfo.getFileName());
        fileNameLabel.setFont(fileNameLabel.getFont().deriveFont(Font.BOLD, 13f));
        topPanel.add(fileNameLabel, BorderLayout.WEST);

        // 变更类型标签
        JBLabel changeTypeLabel = new JBLabel(getChangeTypeIcon(fileInfo.getChangeType()) + " " + fileInfo.getChangeType());
        changeTypeLabel.setFont(changeTypeLabel.getFont().deriveFont(11f));
        changeTypeLabel.setForeground(getChangeTypeColor(fileInfo.getChangeType()));
        topPanel.add(changeTypeLabel, BorderLayout.EAST);

        infoPanel.add(topPanel, BorderLayout.NORTH);

        // 文件路径
        JBLabel pathLabel = new JBLabel(fileInfo.getFilePath());
        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.ITALIC, 10f));
        pathLabel.setForeground(JBColor.GRAY);
        infoPanel.add(pathLabel, BorderLayout.SOUTH);

        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建文件信息面板（无复选框，保持兼容性）
     */
    private JBPanel<?> createFileInfoPanel(GitAnalysisService.StagedFileInfo fileInfo) {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBackground(JBColor.background());
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.LIGHT_GRAY, 1),
            JBUI.Borders.empty(8)
        ));

        // 文件名和状态
        JBPanel<?> infoPanel = new JBPanel<>(new BorderLayout());
        infoPanel.setBackground(JBColor.background());

        JBLabel fileNameLabel = new JBLabel(fileInfo.getFileName());
        fileNameLabel.setFont(fileNameLabel.getFont().deriveFont(Font.BOLD, 13f));
        infoPanel.add(fileNameLabel, BorderLayout.WEST);

        // 变更类型标签
        JBLabel changeTypeLabel = new JBLabel(getChangeTypeIcon(fileInfo.getChangeType()) + " " + fileInfo.getChangeType());
        changeTypeLabel.setFont(changeTypeLabel.getFont().deriveFont(11f));
        changeTypeLabel.setForeground(getChangeTypeColor(fileInfo.getChangeType()));
        infoPanel.add(changeTypeLabel, BorderLayout.EAST);

        panel.add(infoPanel, BorderLayout.NORTH);

        // 文件路径
        JBLabel pathLabel = new JBLabel(fileInfo.getFilePath());
        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.ITALIC, 10f));
        pathLabel.setForeground(JBColor.GRAY);
        panel.add(pathLabel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 获取变更类型图标
     */
    private String getChangeTypeIcon(String changeType) {
        switch (changeType) {
            case "新增": return "➕";
            case "修改": return "✏️";
            case "删除": return "❌";
            case "移动": return "📦";
            default: return "❓";
        }
    }

    /**
     * 获取变更类型颜色
     */
    private Color getChangeTypeColor(String changeType) {
        switch (changeType) {
            case "新增": return JBColor.GREEN;
            case "修改": return JBColor.BLUE;
            case "删除": return JBColor.RED;
            case "移动": return JBColor.ORANGE;
            default: return JBColor.GRAY;
        }
    }

    /**
     * 显示审查结果
     */
    public void displayResult(PreCommitReviewResponse response) {
        // 显示结果面板
        showResultPanel();

        // 使用增强结果面板显示结果
        enhancedResultPanel.displayResult(response);

        // 添加到历史记录
        try {
            GitAnalysisService gitService = GitAnalysisService.getInstance();
            List<GitAnalysisService.StagedFileInfo> stagedFiles = gitService.getStagedFiles(project);
            historyService.addReviewRecord(response, project.getName(), stagedFiles.size());
        } catch (Exception e) {
            // 历史记录添加失败不影响主要功能
        }
    }

    /**
     * 显示结果面板
     */
    private void showResultPanel() {
        Container parent = enhancedResultPanel.getParent();
        if (parent instanceof JBPanel) {
            CardLayout cardLayout = (CardLayout) parent.getLayout();
            cardLayout.show(parent, "RESULT");
        }
    }

    /**
     * 显示错误面板
     */
    private void showErrorPanel() {
        Container parent = errorDisplayPanel.getParent();
        if (parent instanceof JBPanel) {
            CardLayout cardLayout = (CardLayout) parent.getLayout();
            cardLayout.show(parent, "ERROR");
        }
    }

    /**
     * 显示错误信息
     */
    public void displayError(String title, String details, Runnable retryAction) {
        showErrorPanel();
        errorDisplayPanel.showError(title, details, retryAction);
    }

    /**
     * 显示ReviewException错误
     */
    public void displayError(ReviewException exception, Runnable retryAction) {
        showErrorPanel();
        errorDisplayPanel.showError(exception, retryAction);
    }

    /**
     * 清除结果
     */
    public void clearResults() {
        showResultPanel();
        enhancedResultPanel.clearResults();
    }

    /**
     * 全选/全不选文件
     */
    private void selectAllFiles(boolean selected) {
        for (JCheckBox checkBox : fileCheckBoxes) {
            checkBox.setSelected(selected);
        }
    }

    /**
     * 获取选中的文件
     */
    private List<GitAnalysisService.StagedFileInfo> getSelectedFiles() {
        List<GitAnalysisService.StagedFileInfo> selectedFiles = new ArrayList<>();
        for (JCheckBox checkBox : fileCheckBoxes) {
            if (checkBox.isSelected()) {
                GitAnalysisService.StagedFileInfo fileInfo =
                    (GitAnalysisService.StagedFileInfo) checkBox.getClientProperty("fileInfo");
                if (fileInfo != null) {
                    selectedFiles.add(fileInfo);
                }
            }
        }
        return selectedFiles;
    }

    /**
     * 审查Action监听器
     */
    private class ReviewActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 获取选中的文件
            List<GitAnalysisService.StagedFileInfo> selectedStagedFiles = getSelectedFiles();

            if (selectedStagedFiles.isEmpty()) {
                Messages.showInfoMessage(project,
                    "没有选择任何文件进行审查。\n请勾选要审查的文件。",
                    "未选择文件");
                return;
            }

            // 转换为VirtualFile列表
            List<VirtualFile> selectedFiles = new ArrayList<>();
            for (GitAnalysisService.StagedFileInfo stagedFile : selectedStagedFiles) {
                VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(stagedFile.getFilePath());
                if (vf != null) {
                    selectedFiles.add(vf);
                }
            }

            if (selectedFiles.isEmpty()) {
                Messages.showInfoMessage(project,
                    "无法找到选中的文件。\n请检查文件是否存在。",
                    "文件未找到");
                return;
            }

            // 开始审查前先清空之前的结果
            clearResults();

            // 禁用审查按钮，防止重复点击
            reviewButton.setEnabled(false);
            reviewButton.setText("🔄 审查中...");

            // 在后台执行审查
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "正在审查选定文件...", true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    indicator.setText("正在审查 " + selectedFiles.size() + " 个文件...");

                    try {
                        PreCommitReviewService reviewService = PreCommitReviewService.getInstance();
                        PreCommitReviewResponse response = reviewService.reviewSelectedFiles(project, selectedFiles).get();

                        ApplicationManager.getApplication().invokeLater(() -> {
                            displayResult(response);
                            Messages.showInfoMessage(project,
                                String.format("代码审查完成！\n审查文件: %d 个\n质量评分: %d/100 (%s)\n建议数量: %d条",
                                    selectedFiles.size(), response.getQualityScore(), response.getQualityLevel(), response.getSuggestionCount()),
                                "审查完成");
                        });

                    } catch (Exception ex) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            // 使用新的错误显示面板
                            if (ex instanceof ReviewException) {
                                displayError((ReviewException) ex, () -> actionPerformed(e));
                            } else {
                                displayError("审查失败", ex.getMessage(), () -> actionPerformed(e));
                            }
                        });
                    } finally {
                        // 恢复审查按钮状态
                        ApplicationManager.getApplication().invokeLater(() -> {
                            reviewButton.setEnabled(true);
                            reviewButton.setText("🎯 审查选中文件");
                        });
                    }
                }
            });
        }
    }
}
