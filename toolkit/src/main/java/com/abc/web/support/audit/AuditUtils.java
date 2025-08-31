package com.abc.web.support.audit;

import lombok.experimental.UtilityClass;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审计工具类
 * 提供便捷的审计操作方法
 * 
 * @author toolkit
 * @since 1.0.0
 */
@UtilityClass
public class AuditUtils {
    
    private static AuditService auditService;
    
    /**
     * 记录登录事件
     */
    public static void recordLogin(String userId, String username, String clientIp, boolean success) {
        AuditEvent event = AuditEvent.create()
                .setUserId(userId)
                .setUsername(username)
                .setEventType(AuditEventType.AUTHENTICATION)
                .setOperation("LOGIN")
                .setDescription("用户登录")
                .setClientIp(clientIp);
        
        if (success) {
            event.success();
        } else {
            event.failure("登录失败");
        }
        
        getAuditService().saveAuditEventAsync(event);
    }
    
    /**
     * 记录登出事件
     */
    public static void recordLogout(String userId, String username, String clientIp) {
        AuditEvent event = AuditEvent.create()
                .setUserId(userId)
                .setUsername(username)
                .setEventType(AuditEventType.AUTHENTICATION)
                .setOperation("LOGOUT")
                .setDescription("用户登出")
                .setClientIp(clientIp)
                .success();
        
        getAuditService().saveAuditEventAsync(event);
    }
    
    /**
     * 记录数据创建事件
     */
    public static void recordCreate(String userId, String resourceType, String resourceId, 
                                   Object data, String description) {
        AuditEvent event = AuditEvent.create()
                .setUserId(userId)
                .setEventType(AuditEventType.DATA_OPERATION)
                .setOperation("CREATE")
                .setResourceType(resourceType)
                .setResourceId(resourceId)
                .setDescription(description)
                .setAfterData(serializeData(data))
                .success();
        
        getAuditService().saveAuditEventAsync(event);
    }
    
    /**
     * 记录数据更新事件
     */
    public static void recordUpdate(String userId, String resourceType, String resourceId,
                                   Object beforeData, Object afterData, String description) {
        AuditEvent event = AuditEvent.create()
                .setUserId(userId)
                .setEventType(AuditEventType.DATA_OPERATION)
                .setOperation("UPDATE")
                .setResourceType(resourceType)
                .setResourceId(resourceId)
                .setDescription(description)
                .setBeforeData(serializeData(beforeData))
                .setAfterData(serializeData(afterData))
                .success();
        
        getAuditService().saveAuditEventAsync(event);
    }
    
    /**
     * 记录数据删除事件
     */
    public static void recordDelete(String userId, String resourceType, String resourceId,
                                   Object data, String description) {
        AuditEvent event = AuditEvent.create()
                .setUserId(userId)
                .setEventType(AuditEventType.DATA_OPERATION)
                .setOperation("DELETE")
                .setResourceType(resourceType)
                .setResourceId(resourceId)
                .setDescription(description)
                .setBeforeData(serializeData(data))
                .success();
        
        getAuditService().saveAuditEventAsync(event);
    }
    
    /**
     * 记录权限变更事件
     */
    public static void recordPermissionChange(String userId, String targetUserId, String operation,
                                            String permissions, String description) {
        AuditEvent event = AuditEvent.create()
                .setUserId(userId)
                .setEventType(AuditEventType.AUTHORIZATION)
                .setOperation(operation)
                .setResourceType("PERMISSION")
                .setResourceId(targetUserId)
                .setDescription(description)
                .setAfterData(permissions)
                .success();
        
        getAuditService().saveAuditEventAsync(event);
    }
    
    /**
     * 记录系统配置变更事件
     */
    public static void recordConfigChange(String userId, String configKey, String oldValue,
                                        String newValue, String description) {
        AuditEvent event = AuditEvent.create()
                .setUserId(userId)
                .setEventType(AuditEventType.SYSTEM_CONFIG)
                .setOperation("CONFIG_CHANGE")
                .setResourceType("CONFIG")
                .setResourceId(configKey)
                .setDescription(description)
                .setBeforeData(oldValue)
                .setAfterData(newValue)
                .success();
        
        getAuditService().saveAuditEventAsync(event);
    }
    
    /**
     * 记录安全事件
     */
    public static void recordSecurityEvent(String userId, String eventType, String description,
                                         String clientIp, boolean success) {
        AuditEvent event = AuditEvent.create()
                .setUserId(userId)
                .setEventType(AuditEventType.SECURITY)
                .setOperation(eventType)
                .setDescription(description)
                .setClientIp(clientIp);
        
        if (success) {
            event.success();
        } else {
            event.failure(description);
        }
        
        getAuditService().saveAuditEventAsync(event);
    }
    
    /**
     * 记录文件操作事件
     */
    public static void recordFileOperation(String userId, String operation, String fileName,
                                         String filePath, long fileSize, String description) {
        AuditEvent event = AuditEvent.create()
                .setUserId(userId)
                .setEventType(AuditEventType.FILE_OPERATION)
                .setOperation(operation)
                .setResourceType("FILE")
                .setResourceId(fileName)
                .setDescription(description)
                .addAttribute("filePath", filePath)
                .addAttribute("fileSize", fileSize)
                .success();
        
        getAuditService().saveAuditEventAsync(event);
    }
    
    /**
     * 记录API调用事件
     */
    public static void recordApiCall(String userId, String apiPath, String method, 
                                   String requestParams, String response, long duration,
                                   boolean success, String errorMessage) {
        AuditEvent event = AuditEvent.create()
                .setUserId(userId)
                .setEventType(AuditEventType.API_CALL)
                .setOperation(method + " " + apiPath)
                .setResourceType("API")
                .setResourceId(apiPath)
                .setDescription("API调用")
                .setBeforeData(requestParams)
                .setAfterData(response)
                .setDuration(duration);
        
        if (success) {
            event.success();
        } else {
            event.failure(errorMessage);
        }
        
        getAuditService().saveAuditEventAsync(event);
    }
    
    /**
     * 记录自定义事件
     */
    public static void recordCustomEvent(String userId, AuditEventType eventType, String operation,
                                       String description, Map<String, Object> attributes) {
        AuditEvent event = AuditEvent.create()
                .setUserId(userId)
                .setEventType(eventType)
                .setOperation(operation)
                .setDescription(description)
                .setAttributes(attributes)
                .success();
        
        getAuditService().saveAuditEventAsync(event);
    }
    
    /**
     * 构建审计事件
     */
    public static AuditEventBuilder builder() {
        return new AuditEventBuilder();
    }
    
    /**
     * 序列化数据
     */
    private static String serializeData(Object data) {
        if (data == null) {
            return null;
        }
        
        try {
            // 简单实现，实际项目中可以使用Jackson等
            return data.toString();
        } catch (Exception e) {
            return "序列化失败: " + e.getMessage();
        }
    }
    
    /**
     * 获取审计服务
     */
    private static AuditService getAuditService() {
        if (auditService == null) {
            throw new IllegalStateException("AuditService未初始化，请确保Spring容器已启动");
        }
        return auditService;
    }
    
    /**
     * 审计事件构建器
     */
    public static class AuditEventBuilder {
        private final AuditEvent event = AuditEvent.create();
        
        public AuditEventBuilder userId(String userId) {
            event.setUserId(userId);
            return this;
        }
        
        public AuditEventBuilder username(String username) {
            event.setUsername(username);
            return this;
        }
        
        public AuditEventBuilder eventType(AuditEventType eventType) {
            event.setEventType(eventType);
            return this;
        }
        
        public AuditEventBuilder operation(String operation) {
            event.setOperation(operation);
            return this;
        }
        
        public AuditEventBuilder resourceType(String resourceType) {
            event.setResourceType(resourceType);
            return this;
        }
        
        public AuditEventBuilder resourceId(String resourceId) {
            event.setResourceId(resourceId);
            return this;
        }
        
        public AuditEventBuilder description(String description) {
            event.setDescription(description);
            return this;
        }
        
        public AuditEventBuilder beforeData(Object data) {
            event.setBeforeData(serializeData(data));
            return this;
        }
        
        public AuditEventBuilder afterData(Object data) {
            event.setAfterData(serializeData(data));
            return this;
        }
        
        public AuditEventBuilder clientIp(String clientIp) {
            event.setClientIp(clientIp);
            return this;
        }
        
        public AuditEventBuilder attribute(String key, Object value) {
            event.addAttribute(key, value);
            return this;
        }
        
        public AuditEventBuilder success() {
            event.success();
            return this;
        }
        
        public AuditEventBuilder failure(String errorMessage) {
            event.failure(errorMessage);
            return this;
        }
        
        public void save() {
            getAuditService().saveAuditEventAsync(event);
        }
        
        public void saveSync() {
            getAuditService().saveAuditEvent(event);
        }
        
        public AuditEvent build() {
            return event;
        }
    }
    
    /**
     * Spring上下文持有者，用于获取AuditService
     */
    @Component
    static class AuditServiceHolder implements ApplicationContextAware {
        
        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            auditService = applicationContext.getBean(AuditService.class);
        }
    }
}