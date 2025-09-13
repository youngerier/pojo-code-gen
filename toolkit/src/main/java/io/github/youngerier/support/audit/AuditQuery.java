package io.github.youngerier.support.audit;

import io.github.youngerier.support.AbstractPageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计查询条件
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditQuery extends AbstractPageQuery {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
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
     * 操作结果
     */
    private AuditResult result;
    
    /**
     * 模块名称
     */
    private String moduleName;
    
    /**
     * 客户端IP
     */
    private String clientIp;
    
    /**
     * 请求路径
     */
    private String requestPath;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 关键词搜索
     */
    private String keyword;
    
    /**
     * 事件类型列表
     */
    private List<AuditEventType> eventTypes;
    
    /**
     * 操作类型列表
     */
    private List<String> operations;
    
    /**
     * 用户角色列表
     */
    private List<String> userRoles;
    
    /**
     * 最小耗时（毫秒）
     */
    private Long minDuration;
    
    /**
     * 最大耗时（毫秒）
     */
    private Long maxDuration;
}