package com.jdc.tools.precommit.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import com.jdc.tools.precommit.util.FileTypeFilter;

import javax.swing.*;

/**
 * 文件树单元格渲染器
 * 为不同类型的文件显示相应的图标和样式
 */
public class FileTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer {

    @Override
    public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (!(value instanceof CheckedTreeNode)) {
            return;
        }

        CheckedTreeNode node = (CheckedTreeNode) value;
        Object userObject = node.getUserObject();

        if (userObject instanceof VirtualFile) {
            VirtualFile file = (VirtualFile) userObject;
            
            // 设置文件图标
            Icon icon = getFileIcon(file);
            getTextRenderer().setIcon(icon);
            
            // 设置文件名
            String fileName = file.getName();
            getTextRenderer().append(fileName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            
            // 添加文件路径信息
            String relativePath = getRelativePath(file);
            if (!relativePath.isEmpty()) {
                getTextRenderer().append(" (" + relativePath + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
            
            // 添加文件类型和大小信息
            String fileTypeDesc = FileTypeFilter.getFileTypeDescription(file);
            long fileSize = file.getLength();
            String sizeText = formatFileSize(fileSize);
            getTextRenderer().append(" [" + fileTypeDesc + ", " + sizeText + "]", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
            
        } else if (userObject instanceof String) {
            // 根节点
            getTextRenderer().setIcon(AllIcons.Nodes.Folder);
            getTextRenderer().append("📁 " + (String) userObject + " (仅显示代码文件)", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        }
    }

    private Icon getFileIcon(VirtualFile file) {
        String extension = file.getExtension();
        if (extension == null) {
            return AllIcons.FileTypes.Text;
        }

        // 根据文件扩展名返回相应图标
        switch (extension.toLowerCase()) {
            case "java":
                return AllIcons.FileTypes.Java;
            case "kt":
                return AllIcons.FileTypes.Unknown; // Kotlin
            case "js":
            case "ts":
                return AllIcons.FileTypes.JavaScript;
            case "html":
            case "htm":
                return AllIcons.FileTypes.Html;
            case "css":
                return AllIcons.FileTypes.Css;
            case "xml":
                return AllIcons.FileTypes.Xml;
            case "json":
                return AllIcons.FileTypes.Json;
            case "yml":
            case "yaml":
                return AllIcons.FileTypes.Yaml;
            case "md":
                return AllIcons.FileTypes.Text;
            case "sql":
                return AllIcons.FileTypes.Unknown;
            case "py":
                return AllIcons.FileTypes.Unknown;
            case "go":
                return AllIcons.FileTypes.Unknown;
            case "rs":
                return AllIcons.FileTypes.Unknown;
            case "cpp":
            case "c":
            case "h":
                return AllIcons.FileTypes.Unknown;
            case "sh":
                return AllIcons.FileTypes.Unknown;
            case "properties":
                return AllIcons.FileTypes.Properties;
            default:
                return AllIcons.FileTypes.Text;
        }
    }

    private String getRelativePath(VirtualFile file) {
        VirtualFile parent = file.getParent();
        if (parent == null) {
            return "";
        }
        
        StringBuilder path = new StringBuilder();
        while (parent != null && !parent.getName().isEmpty()) {
            if (path.length() > 0) {
                path.insert(0, "/");
            }
            path.insert(0, parent.getName());
            parent = parent.getParent();
        }
        
        return path.toString();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
