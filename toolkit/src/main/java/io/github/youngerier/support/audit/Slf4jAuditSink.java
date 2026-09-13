package io.github.youngerier.support.audit;

import lombok.extern.slf4j.Slf4j;

/**
 * 默认审计输出：单行 INFO 日志。需要落库时实现 {@link AuditSink} 覆盖默认 Bean。
 */
@Slf4j
public class Slf4jAuditSink implements AuditSink {

    @Override
    public void save(AuditEvent event) {
        log.info("audit: op={}, type={}, key={}, user={}, success={}, cost={}ms, path={}, ip={}",
                event.getOperation(),
                event.getType(),
                event.getBusinessKey(),
                event.getUserId(),
                event.isSuccess(),
                event.getDurationMs(),
                event.getPath(),
                event.getClientIp());
    }
}
