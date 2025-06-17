package com.jdc.tools.precommit.model;

import java.util.Map;

/**
 * Git提交前代码审查请求模型
 */
public class PreCommitReviewRequest {
    
    /**
     * 项目名称
     */
    private String projectName;
    
    /**
     * 审查类型：pre_commit, single_file
     */
    private String reviewType;
    
    /**
     * 使用的AI模型Key
     */
    private String modelKey;
    
    /**
     * 审查重点：all, security, performance, style, logic
     */
    private String reviewFocus;
    
    /**
     * 分析深度：quick, standard, detailed, comprehensive
     */
    private String analysisDepth;
    
    /**
     * 文件数量
     */
    private Integer fileCount;
    
    /**
     * 文件数据
     * Key: 文件路径
     * Value: 文件信息（包含内容、变动类型、差异等）
     */
    private Map<String, Object> files;
    
    /**
     * 审查描述
     */
    private String description;
    
    /**
     * 请求时间戳
     */
    private Long timestamp;
    
    public PreCommitReviewRequest() {
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public String getReviewType() {
        return reviewType;
    }
    
    public void setReviewType(String reviewType) {
        this.reviewType = reviewType;
    }
    
    public String getModelKey() {
        return modelKey;
    }
    
    public void setModelKey(String modelKey) {
        this.modelKey = modelKey;
    }
    
    public String getReviewFocus() {
        return reviewFocus;
    }
    
    public void setReviewFocus(String reviewFocus) {
        this.reviewFocus = reviewFocus;
    }
    
    public String getAnalysisDepth() {
        return analysisDepth;
    }
    
    public void setAnalysisDepth(String analysisDepth) {
        this.analysisDepth = analysisDepth;
    }
    
    public Integer getFileCount() {
        return fileCount;
    }
    
    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }
    
    public Map<String, Object> getFiles() {
        return files;
    }
    
    public void setFiles(Map<String, Object> files) {
        this.files = files;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("PreCommitReviewRequest{projectName='%s', reviewType='%s', fileCount=%d}", 
            projectName, reviewType, fileCount);
    }
}
