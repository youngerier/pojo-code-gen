package io.github.youngerier.support.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * traceId 上下文工具：基于 SLF4J MDC 在同一请求线程内共享链路 ID。
 */
public final class TraceContext {

    /**
     * MDC 中的 traceId key，logback pattern 中使用 %X{traceId} 输出
     */
    public static final String TRACE_ID = "traceId";

    /**
     * 上游传递 traceId 的请求头名称
     */
    public static final String TRACE_ID_HEADER = "X-Request-ID";

    private TraceContext() {
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
    }

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
