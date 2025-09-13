package io.github.youngerier.support.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审计事件类型枚举
 * 
 * @author toolkit
 * @since 1.0.0
 */
@AllArgsConstructor
@Getter
public enum AuditEventType {
    
    /**
     * 用户认证事件
     */
    AUTHENTICATION("用户认证"),
    
    /**
     * 用户登录事件
     */
    LOGIN("用户登录"),
    
    /**
     * 用户登出事件
     */
    LOGOUT("用户登出"),
    
    /**
     * 用户授权事件
     */
    AUTHORIZATION("用户授权"),
    
    /**
     * 数据操作事件
     */
    DATA_OPERATION("数据操作"),
    
    /**
     * 创建操作
     */
    CREATE("创建操作"),
    
    /**
     * 更新操作
     */
    UPDATE("更新操作"),
    
    /**
     * 删除操作
     */
    DELETE("删除操作"),
    
    /**
     * 查询操作
     */
    READ("查询操作"),
    
    /**
     * 系统配置事件
     */
    SYSTEM_CONFIG("系统配置"),
    
    /**
     * 安全事件
     */
    SECURITY("安全事件"),
    
    /**
     * 业务操作事件
     */
    BUSINESS_OPERATION("业务操作"),
    
    /**
     * 文件操作事件
     */
    FILE_OPERATION("文件操作"),
    
    /**
     * API调用事件
     */
    API_CALL("API调用"),
    
    /**
     * 定时任务事件
     */
    SCHEDULED_TASK("定时任务"),
    
    /**
     * 系统异常事件
     */
    SYSTEM_ERROR("系统异常");
    
    private final String description;

}