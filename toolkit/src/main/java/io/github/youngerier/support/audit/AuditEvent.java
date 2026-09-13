package io.github.youngerier.support.audit;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 审计事件
 */
@Getter
@Builder
public class AuditEvent {

    /** 事件 ID */
    private final String id;

    /** 发生时间 */
    private final Instant time;

    /** 链路 ID（取自 MDC） */
    private final String traceId;

    /** 操作人 */
    private final String userId;

    /** 操作名称 */
    private final String operation;

    /** 操作类型 */
    private final String type;

    /** 业务主键（SpEL 解析结果） */
    private final String businessKey;

    /** 请求路径 */
    private final String path;

    /** HTTP 方法 */
    private final String httpMethod;

    /** 客户端 IP */
    private final String clientIp;

    /** 方法参数（JSON，已脱敏） */
    private final String parameters;

    /** 返回值（JSON） */
    private final String result;

    /** 是否成功 */
    private final boolean success;

    /** 失败原因 */
    private final String errorMessage;

    /** 耗时（毫秒） */
    private final long durationMs;
}
