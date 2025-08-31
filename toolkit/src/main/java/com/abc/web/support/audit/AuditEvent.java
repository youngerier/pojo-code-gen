package com.abc.web.support.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审计事件实体
 * 记录系统中的各种操作和事件
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    
    /**
     * 事件ID
     */
    private String eventId;
    
    /**
     * 操作用户ID
     */
    private String userId;
    
    /**
     * 操作用户名
     */
    private String username;
    
    /**
     * 用户角色
     */
    private String userRole;
    
    /**
     * 事件类型
     */
    private AuditEventType eventType;
    
    /**
     * 操作类型
     */
    private String operation;
    
    /**
     * 资源类型
     */
    private String resourceType;
    
    /**
     * 资源ID
     */
    private String resourceId;
    
    /**
     * 操作描述
     */
    private String description;
    
    /**
     * 操作结果
     */
    private AuditResult result;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 操作前数据
     */
    private String beforeData;
    
    /**
     * 操作后数据
     */
    private String afterData;
    
    /**
     * 客户端IP
     */
    private String clientIp;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 请求路径
     */
    private String requestPath;
    
    /**
     * 请求方法
     */
    private String requestMethod;
    
    /**
     * 耗时（毫秒）
     */
    private Long duration;
    
    /**
     * 事件时间
     */
    private LocalDateTime eventTime;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 模块名称
     */
    private String moduleName;
    
    /**
     * 业务标识
     */
    private String businessKey;
    
    /**
     * 扩展属性
     */
    private Map<String, Object> attributes;
    
    /**
     * 创建审计事件
     */
    public static AuditEvent create() {
        return AuditEvent.builder()
                .eventId(generateEventId())
                .eventTime(LocalDateTime.now())
                .result(AuditResult.SUCCESS)
                .build();
    }
    
    /**
     * 生成事件ID
     */
    private static String generateEventId() {
        return "AUDIT_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString((int)(Math.random() * 0x10000));
    }
    
    /**
     * 设置操作成功
     */
    public AuditEvent success() {
        this.result = AuditResult.SUCCESS;
        return this;
    }
    
    /**
     * 设置操作失败
     */
    public AuditEvent failure(String errorMessage) {
        this.result = AuditResult.FAILURE;
        this.errorMessage = errorMessage;
        return this;
    }
    
    /**
     * 添加扩展属性
     */
    public AuditEvent addAttribute(String key, Object value) {
        if (this.attributes == null) {
            this.attributes = new java.util.HashMap<>();
        }
        this.attributes.put(key, value);
        return this;
    }
    
    // 链式调用的setter方法
    public AuditEvent setUserId(String userId) {
        this.userId = userId;
        return this;
    }
    
    public AuditEvent setUsername(String username) {
        this.username = username;
        return this;
    }
    
    public AuditEvent setEventType(AuditEventType eventType) {
        this.eventType = eventType;
        return this;
    }
    
    public AuditEvent setOperation(String operation) {
        this.operation = operation;
        return this;
    }
    
    public AuditEvent setResourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }
    
    public AuditEvent setResourceId(String resourceId) {
        this.resourceId = resourceId;
        return this;
    }
    
    public AuditEvent setDescription(String description) {
        this.description = description;
        return this;
    }
    
    public AuditEvent setBeforeData(String beforeData) {
        this.beforeData = beforeData;
        return this;
    }
    
    public AuditEvent setAfterData(String afterData) {
        this.afterData = afterData;
        return this;
    }
    
    public AuditEvent setClientIp(String clientIp) {
        this.clientIp = clientIp;
        return this;
    }
    
    public AuditEvent setDuration(Long duration) {
        this.duration = duration;
        return this;
    }
    
    public AuditEvent setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }
    
    public AuditEvent setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
        return this;
    }
    
    public AuditEvent setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }
}