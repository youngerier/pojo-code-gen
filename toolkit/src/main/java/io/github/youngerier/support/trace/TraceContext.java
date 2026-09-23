package io.github.youngerier.support.trace;

import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

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

    /**
     * 设置当前线程的 traceId。
     *
     * @throws IllegalArgumentException traceId 为 null 或空白（SLF4J MDC 不接受 null，
     *                                  空白值也会让 ensureTraceId 等判断失效）
     */
    public static void setTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be null or blank");
        }
        MDC.put(TRACE_ID, traceId);
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
    }

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 确保当前线程存在非空白 traceId，没有则生成一个。适用于定时任务、消息消费者等非 HTTP 入口。
     *
     * @return 当前（可能刚生成的）traceId
     */
    public static String ensureTraceId() {
        String traceId = getTraceId();
        if (traceId == null || traceId.isBlank()) {
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
                restore(previous);
            }
        };
    }

    /**
     * Callable 版本的 {@link #wrap(Runnable)}：提交时复制当前 MDC，执行线程上恢复。
     *
     * @return 提交到线程池的 Callable；执行时原样返回业务结果或抛出业务异常
     */
    public static <V> Callable<V> wrap(Callable<V> task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (context != null) {
                MDC.setContextMap(context);
            }
            try {
                return task.call();
            } finally {
                restore(previous);
            }
        };
    }

    /**
     * 恢复 MDC 到任务执行前的状态：之前有内容则整体还原，之前为空则清空，
     * 保证借用的池线程不会残留本次任务的上下文。供同包工具类共用。
     */
    static void restore(Map<String, String> previous) {
        if (previous != null) {
            MDC.setContextMap(previous);
        } else {
            MDC.clear();
        }
    }
}
