package com.jdc.tools.precommit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.jdc.tools.precommit.model.PreCommitReviewResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 审查历史服务
 * 负责本地缓存和管理审查历史记录
 */
@Service
public final class ReviewHistoryService {
    
    private static final Logger LOG = Logger.getInstance(ReviewHistoryService.class);
    private static final String HISTORY_KEY = "JDC_PRECOMMIT_REVIEW_HISTORY";
    private static final int MAX_HISTORY_SIZE = 50; // 最多保存50条记录
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public static ReviewHistoryService getInstance() {
        return ApplicationManager.getApplication().getService(ReviewHistoryService.class);
    }
    
    /**
     * 添加审查记录到历史
     */
    public void addReviewRecord(PreCommitReviewResponse response, String projectName, int fileCount) {
        try {
            List<ReviewHistoryRecord> history = loadHistory();
            
            // 创建新的历史记录
            ReviewHistoryRecord record = new ReviewHistoryRecord();
            record.setReviewId(response.getReviewId());
            record.setTimestamp(new Date());
            record.setProjectName(projectName);
            record.setFileCount(fileCount);
            record.setQualityScore(response.getQualityScore());
            record.setQualityLevel(response.getQualityLevel());
            record.setSuggestionCount(response.getSuggestionCount());
            record.setProcessingTime(response.getProcessingTime());
            record.setSummary(generateSummary(response));
            
            // 添加到历史记录开头
            history.add(0, record);
            
            // 限制历史记录数量
            if (history.size() > MAX_HISTORY_SIZE) {
                history = history.subList(0, MAX_HISTORY_SIZE);
            }
            
            // 保存到本地
            saveHistory(history);
            
            LOG.info("已添加审查记录到历史: " + record.getReviewId());
            
        } catch (Exception e) {
            LOG.error("添加审查历史记录失败", e);
        }
    }
    
    /**
     * 获取审查历史记录
     */
    public List<ReviewHistoryRecord> getHistory() {
        return loadHistory();
    }
    
    /**
     * 清空审查历史
     */
    public void clearHistory() {
        try {
            PropertiesComponent properties = PropertiesComponent.getInstance();
            properties.unsetValue(HISTORY_KEY);
            LOG.info("已清空审查历史记录");
        } catch (Exception e) {
            LOG.error("清空审查历史失败", e);
        }
    }
    
    /**
     * 删除指定的审查记录
     */
    public void deleteRecord(String reviewId) {
        try {
            List<ReviewHistoryRecord> history = loadHistory();
            history.removeIf(record -> reviewId.equals(record.getReviewId()));
            saveHistory(history);
            LOG.info("已删除审查记录: " + reviewId);
        } catch (Exception e) {
            LOG.error("删除审查记录失败", e);
        }
    }
    
    /**
     * 从本地加载历史记录
     */
    private List<ReviewHistoryRecord> loadHistory() {
        try {
            PropertiesComponent properties = PropertiesComponent.getInstance();
            String historyJson = properties.getValue(HISTORY_KEY);
            
            if (historyJson != null && !historyJson.trim().isEmpty()) {
                TypeReference<List<ReviewHistoryRecord>> typeRef = new TypeReference<List<ReviewHistoryRecord>>() {};
                return objectMapper.readValue(historyJson, typeRef);
            }
            
        } catch (Exception e) {
            LOG.warn("加载审查历史失败", e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 保存历史记录到本地
     */
    private void saveHistory(List<ReviewHistoryRecord> history) {
        try {
            String historyJson = objectMapper.writeValueAsString(history);
            PropertiesComponent properties = PropertiesComponent.getInstance();
            properties.setValue(HISTORY_KEY, historyJson);
        } catch (Exception e) {
            LOG.error("保存审查历史失败", e);
        }
    }
    
    /**
     * 生成审查摘要
     */
    private String generateSummary(PreCommitReviewResponse response) {
        StringBuilder summary = new StringBuilder();
        
        if (response.getQualityScore() != null) {
            summary.append("质量评分: ").append(response.getQualityScore()).append("/100");
        }
        
        if (response.getSuggestionCount() > 0) {
            summary.append(", ").append(response.getSuggestionCount()).append("条建议");
        }
        
        if (response.getProcessingTime() != null) {
            summary.append(", 耗时: ").append(response.getFormattedProcessingTime());
        }
        
        return summary.toString();
    }
    
    /**
     * 审查历史记录数据类
     */
    public static class ReviewHistoryRecord {
        private String reviewId;
        private Date timestamp;
        private String projectName;
        private int fileCount;
        private Integer qualityScore;
        private String qualityLevel;
        private int suggestionCount;
        private Long processingTime;
        private String summary;
        
        // 默认构造函数
        public ReviewHistoryRecord() {}
        
        // Getters and Setters
        public String getReviewId() { return reviewId; }
        public void setReviewId(String reviewId) { this.reviewId = reviewId; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
        
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        
        public int getFileCount() { return fileCount; }
        public void setFileCount(int fileCount) { this.fileCount = fileCount; }
        
        public Integer getQualityScore() { return qualityScore; }
        public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }
        
        public String getQualityLevel() { return qualityLevel; }
        public void setQualityLevel(String qualityLevel) { this.qualityLevel = qualityLevel; }
        
        public int getSuggestionCount() { return suggestionCount; }
        public void setSuggestionCount(int suggestionCount) { this.suggestionCount = suggestionCount; }
        
        public Long getProcessingTime() { return processingTime; }
        public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        @Override
        public String toString() {
            return String.format("ReviewHistoryRecord{reviewId='%s', projectName='%s', qualityScore=%d}", 
                reviewId, projectName, qualityScore);
        }
    }
}
