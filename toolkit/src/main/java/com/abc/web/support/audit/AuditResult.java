package com.abc.web.support.audit;

/**
 * 审计结果枚举
 * 
 * @author toolkit
 * @since 1.0.0
 */
public enum AuditResult {
    
    /**
     * 成功
     */
    SUCCESS("成功"),
    
    /**
     * 失败
     */
    FAILURE("失败"),
    
    /**
     * 部分成功
     */
    PARTIAL_SUCCESS("部分成功"),
    
    /**
     * 已取消
     */
    CANCELLED("已取消"),
    
    /**
     * 超时
     */
    TIMEOUT("超时");
    
    private final String description;
    
    AuditResult(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}