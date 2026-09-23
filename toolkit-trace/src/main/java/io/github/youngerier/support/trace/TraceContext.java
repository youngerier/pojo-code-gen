package io.github.youngerier.support.trace;

import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * traceId 上下文工具：基于 SLF4J MDC 在同一请求线程内共享链路 ID。
 *
 * <p>上游传入的 traceId（HTTP 请求头、MQ 用户属性）一律先经过
 * {@link #sanitizeTraceId(String)} 白名单校验，非法内容（换行、控制字符、超长）会被丢弃并重新生成，
 * 避免日志伪造与响应头污染。
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

    /**
     * 可接受的上游 traceId 最大长度
     */
    public static final int MAX_TRACE_ID_LENGTH = 128;

    /**
     * 合法 traceId 字符集：字母、数字、下划线、短横线、点，长度 1~{@value #MAX_TRACE_ID_LENGTH}
     */
    private static final Pattern VALID_TRACE_ID =
            Pattern.compile("[A-Za-z0-9._-]{1," + MAX_TRACE_ID_LENGTH + "}");

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
     * 校验并规整上游传入的 traceId：去除首尾空白，仅接受
     * {@code [A-Za-z0-9._-]{1,128}} 形式的取值。
     *
     * @param traceId 上游传入的原始值，可为 null
     * @return 规整后的 traceId；为空或非法时返回 {@code null}（调用方应改为生成新 id）
     */
    public static String sanitizeTraceId(String traceId) {
        if (traceId == null) {
            return null;
        }
        String candidate = traceId.trim();
        return VALID_TRACE_ID.matcher(candidate).matches() ? candidate : null;
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
     * 快照当前线程的完整 MDC，配合 {@link #restore(Map)} 使用，
     * 以便在借用池化线程时「还原」而非「清空」，不误删其他组件写入的键。
     *
     * @return 当前 MDC 的副本；当前线程无 MDC 时返回 {@code null}
     */
    public static Map<String, String> snapshot() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * 把 MDC 恢复到 {@link #snapshot()} 得到的快照：之前有内容则整体还原，之前为空则清空。
     *
     * @param previous {@link #snapshot()} 的返回值，可为 null
     */
    public static void restore(Map<String, String> previous) {
        if (previous != null) {
            MDC.setContextMap(previous);
        } else {
            MDC.clear();
        }
    }

    /**
     * 包装任务：提交时复制当前 MDC（含 traceId），执行线程上恢复，结束后恢复原状。
     * 适用于未统一配置 TaskDecorator 的线程池、@Async 方法或消息消费场景。
     */
    public static Runnable wrap(Runnable task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = snapshot();
            // 必须无条件恢复提交时刻的上下文（没有则清空）：否则借用的池线程会把
            // 上一条任务的 MDC 残留串进本次任务。restore 对 null 安全，
            // 不依赖具体 MDCAdapter 对 setContextMap(null) 的实现。
            restore(context);
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
     * <p>刻意不叫 {@code wrap}：与 {@code wrap(Runnable)} 重载会让
     * {@code wrap(() -> list.add(x))} 这类「表达式 lambda」产生歧义（既可作语句也可作返回值），
     * 使用方必须写强制转换才能编译。
     *
     * @return 提交到线程池的 Callable；执行时原样返回业务结果或抛出业务异常
     */
    public static <V> Callable<V> wrapCallable(Callable<V> task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = snapshot();
            restore(context);
            try {
                return task.call();
            } finally {
                restore(previous);
            }
        };
    }
}
