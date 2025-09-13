package io.github.youngerier.support.audit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 审计服务接口
 * 
 * @author toolkit
 * @since 1.0.0
 */
public interface AuditService {
    
    /**
     * 同步保存审计事件
     * 
     * @param event 审计事件
     */
    void saveAuditEvent(AuditEvent event);
    
    /**
     * 异步保存审计事件
     * 
     * @param event 审计事件
     * @return CompletableFuture
     */
    CompletableFuture<Void> saveAuditEventAsync(AuditEvent event);
    
    /**
     * 批量保存审计事件
     * 
     * @param events 审计事件列表
     */
    void saveAuditEvents(List<AuditEvent> events);
    
    /**
     * 查询审计事件
     * 
     * @param query 查询条件
     * @return 审计事件列表
     */
    List<AuditEvent> queryAuditEvents(AuditQuery query);
    
    /**
     * 统计审计事件数量
     * 
     * @param query 查询条件
     * @return 事件数量
     */
    long countAuditEvents(AuditQuery query);
    
    /**
     * 根据用户ID查询审计事件
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审计事件列表
     */
    List<AuditEvent> queryByUserId(String userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据资源查询审计事件
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 审计事件列表
     */
    List<AuditEvent> queryByResource(String resourceType, String resourceId);
    
    /**
     * 根据操作类型查询审计事件
     * 
     * @param operation 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审计事件列表
     */
    List<AuditEvent> queryByOperation(String operation, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 清理过期的审计日志
     * 
     * @param beforeTime 清理此时间之前的日志
     * @return 清理的记录数
     */
    int cleanupExpiredAuditEvents(LocalDateTime beforeTime);
    
    /**
     * 导出审计日志
     * 
     * @param query 查询条件
     * @param format 导出格式（CSV, EXCEL, JSON等）
     * @return 导出文件路径
     */
    String exportAuditEvents(AuditQuery query, String format);
}