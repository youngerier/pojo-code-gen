package io.github.youngerier.support.audit;

/**
 * 审计事件输出端。使用方实现本接口落库/上报；
 * 默认提供 {@link Slf4jAuditSink} 仅输出日志。
 */
@FunctionalInterface
public interface AuditSink {

    void save(AuditEvent event);
}
