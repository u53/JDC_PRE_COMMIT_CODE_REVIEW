package com.jdc.tools.precommit.exception;

/**
 * 代码审查异常类
 * 用于封装审查过程中的各种错误信息
 */
public class ReviewException extends RuntimeException {
    
    private final String title;
    private final String details;
    
    public ReviewException(String title, String details) {
        super(title + ": " + details);
        this.title = title;
        this.details = details;
    }
    
    public ReviewException(String title, String details, Throwable cause) {
        super(title + ": " + details, cause);
        this.title = title;
        this.details = details;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDetails() {
        return details;
    }
    
    /**
     * 获取用户友好的错误消息
     */
    public String getUserFriendlyMessage() {
        return title + "\n\n" + details;
    }
}
