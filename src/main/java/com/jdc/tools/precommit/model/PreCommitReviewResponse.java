package com.jdc.tools.precommit.model;

import java.util.List;
import java.util.Map;

/**
 * Git提交前代码审查响应模型
 */
public class PreCommitReviewResponse {

    /**
     * 审查ID
     */
    private String reviewId;

    /**
     * 分析结果
     */
    private String analysis;

    /**
     * 改进建议列表
     */
    private List<String> suggestions;

    /**
     * 结构化建议列表
     */
    private List<Map<String, Object>> structuredSuggestions;

    /**
     * 代码质量评分 (0-100)
     */
    private Integer qualityScore;

    /**
     * 复杂度分析
     */
    private Map<String, Object> complexity;

    /**
     * 处理时间（毫秒）
     */
    private Long processingTime;

    /**
     * 审查时间戳
     */
    private Long timestamp;

    /**
     * 审查状态：success, warning, error
     */
    private String status;

    /**
     * 状态消息
     */
    private String message;

    public PreCommitReviewResponse() {
        this.timestamp = System.currentTimeMillis();
        this.status = "success";
    }

    // Getters and Setters
    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<Map<String, Object>> getStructuredSuggestions() {
        return structuredSuggestions;
    }

    public void setStructuredSuggestions(List<Map<String, Object>> structuredSuggestions) {
        this.structuredSuggestions = structuredSuggestions;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Map<String, Object> getComplexity() {
        return complexity;
    }

    public void setComplexity(Map<String, Object> complexity) {
        this.complexity = complexity;
    }

    public Long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取格式化的处理时间
     */
    public String getFormattedProcessingTime() {
        if (processingTime == null) {
            return "未知";
        }

        if (processingTime < 1000) {
            return processingTime + "ms";
        } else {
            return String.format("%.1fs", processingTime / 1000.0);
        }
    }

    /**
     * 获取质量等级
     */
    public String getQualityLevel() {
        if (qualityScore == null) {
            return "未评分";
        }

        if (qualityScore >= 90) {
            return "优秀";
        } else if (qualityScore >= 80) {
            return "良好";
        } else if (qualityScore >= 70) {
            return "一般";
        } else if (qualityScore >= 60) {
            return "需改进";
        } else {
            return "较差";
        }
    }

    /**
     * 检查是否有建议
     */
    public boolean hasSuggestions() {
        return suggestions != null && !suggestions.isEmpty();
    }

    /**
     * 获取建议数量
     */
    public int getSuggestionCount() {
        return suggestions != null ? suggestions.size() : 0;
    }

    @Override
    public String toString() {
        return String.format("PreCommitReviewResponse{reviewId='%s', qualityScore=%d, suggestionCount=%d, status='%s'}",
            reviewId, qualityScore, getSuggestionCount(), status);
    }
}
