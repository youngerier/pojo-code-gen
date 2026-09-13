package io.github.youngerier.support.trace;

import org.slf4j.MDC;

import java.util.Map;
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
     * 上游传递 traceId 的默认请求头名称
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

    /**
     * 确保当前线程存在 traceId，没有则生成一个。适用于定时任务、消息消费者等非 HTTP 入口。
     *
     * @return 当前（可能刚生成的）traceId
     */
    public static String ensureTraceId() {
        String traceId = getTraceId();
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }

    /**
     * 包装任务：提交时复制当前 MDC（含 traceId），执行线程上恢复，结束后恢复原状。
     * 适用于未统一配置 TaskDecorator 的线程池、@Async 方法或消息消费场景。
     */
    public static Runnable wrap(Runnable task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (context != null) {
                MDC.setContextMap(context);
            }
            try {
                task.run();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
