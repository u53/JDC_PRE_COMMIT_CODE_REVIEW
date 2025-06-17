package com.jdc.tools.precommit.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.jdc.tools.precommit.util.FileTypeFilter;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 文件选择对话框
 * 允许用户选择要进行代码审查的文件
 */
public class FileSelectionDialog extends DialogWrapper {

    private final List<VirtualFile> allFiles;
    private CheckboxTree fileTree;
    private CheckedTreeNode rootNode;
    private JButton selectAllButton;
    private JButton deselectAllButton;
    private JLabel statusLabel;

    public FileSelectionDialog(@NotNull Project project, @NotNull List<VirtualFile> files) {
        super(project);
        this.allFiles = files;
        setTitle("选择要审查的文件");
        setModal(true);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(JBUI.size(600, 400));

        // 创建顶部控制面板
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // 创建文件树
        createFileTree();
        JBScrollPane scrollPane = new JBScrollPane(fileTree);
        scrollPane.setPreferredSize(JBUI.size(580, 300));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 创建底部状态面板
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(JBUI.Borders.empty(5, 10));

        // 全选按钮
        selectAllButton = new JButton("全部选择");
        selectAllButton.setIcon(AllIcons.Actions.Selectall);
        selectAllButton.addActionListener(e -> selectAllFiles(true));

        // 全不选按钮
        deselectAllButton = new JButton("全部取消");
        deselectAllButton.setIcon(AllIcons.Actions.Unselectall);
        deselectAllButton.addActionListener(e -> selectAllFiles(false));

        // 添加说明标签
        JLabel infoLabel = new JLabel("🔍 请选择要进行AI代码审查的文件（已过滤非代码文件）：");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD));

        panel.add(infoLabel);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(selectAllButton);
        panel.add(deselectAllButton);

        return panel;
    }

    private void createFileTree() {
        rootNode = new CheckedTreeNode("暂存区文件");
        
        // 按目录分组文件
        for (VirtualFile file : allFiles) {
            CheckedTreeNode fileNode = new CheckedTreeNode(file);
            fileNode.setChecked(true); // 默认全选
            rootNode.add(fileNode);
        }

        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        fileTree = new CheckboxTree(new FileTreeCellRenderer(), rootNode) {
            @Override
            protected void onNodeStateChanged(CheckedTreeNode node) {
                super.onNodeStateChanged(node);
                updateStatusLabel();
            }
        };
        
        fileTree.setModel(treeModel);
        fileTree.setRootVisible(true);
        // 展开所有节点
        for (int i = 0; i < fileTree.getRowCount(); i++) {
            fileTree.expandRow(i);
        }
        
        // 初始化状态
        updateStatusLabel();
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(JBUI.Borders.empty(5, 10));

        statusLabel = new JLabel();
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
        panel.add(statusLabel);

        return panel;
    }

    private void selectAllFiles(boolean selected) {
        setNodeChecked(rootNode, selected);
        fileTree.repaint();
        updateStatusLabel();
    }

    private void setNodeChecked(CheckedTreeNode node, boolean checked) {
        node.setChecked(checked);
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            CheckedTreeNode child = (CheckedTreeNode) children.nextElement();
            setNodeChecked(child, checked);
        }
    }

    private void updateStatusLabel() {
        int selectedCount = getSelectedFileCount();
        int totalCount = allFiles.size();
        statusLabel.setText(String.format("✅ 已选择 %d / %d 个代码文件", selectedCount, totalCount));

        // 更新OK按钮状态
        setOKActionEnabled(selectedCount > 0);
    }

    private int getSelectedFileCount() {
        int count = 0;
        Enumeration<?> children = rootNode.children();
        while (children.hasMoreElements()) {
            CheckedTreeNode child = (CheckedTreeNode) children.nextElement();
            if (child.isChecked()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取用户选择的文件列表
     */
    public List<VirtualFile> getSelectedFiles() {
        List<VirtualFile> selectedFiles = new ArrayList<>();
        Enumeration<?> children = rootNode.children();
        while (children.hasMoreElements()) {
            CheckedTreeNode child = (CheckedTreeNode) children.nextElement();
            if (child.isChecked()) {
                Object userObject = child.getUserObject();
                if (userObject instanceof VirtualFile) {
                    selectedFiles.add((VirtualFile) userObject);
                }
            }
        }
        return selectedFiles;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        if (getSelectedFileCount() == 0) {
            // 显示警告
            JOptionPane.showMessageDialog(
                getContentPanel(),
                "请至少选择一个文件进行审查",
                "提示",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        super.doOKAction();
    }
}
