package io.github.youngerier.support.trace;

import org.slf4j.MDC;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 跨进程链路透传工具：在消息发送前把 traceId 注入消息载体，在消费端从载体恢复 traceId。
 * <p>
 * 与 {@link TraceContext#wrap(Runnable)} 的线程间 MDC 复制不同，本类面向「消息头 / 用户属性」
 * 这类随消息跨进程传输的载体，本身不绑定任何 MQ 框架，适配其他 MQ 时也可直接复用。
 */
public final class TracePropagator {

    private TracePropagator() {
    }

    /**
     * 生产端：把当前 traceId 写入消息载体，当前线程没有 traceId 时生成一个。
     *
     * @param carrier 载体写入函数，如消息属性的 put(name, value)
     * @return 写入的 traceId
     */
    public static String inject(BiConsumer<String, String> carrier) {
        String traceId = TraceContext.ensureTraceId();
        carrier.accept(TraceContext.TRACE_ID, traceId);
        return traceId;
    }

    /**
     * 消费端：从载体解析 traceId 放入 MDC，载体中没有时生成新的 traceId。
     *
     * @param carrier 载体读取函数，如消息属性的 get(name)
     * @return 当前（可能刚生成的）traceId
     */
    public static String extract(Function<String, String> carrier) {
        String traceId = resolve(carrier);
        if (traceId == null) {
            traceId = TraceContext.generateTraceId();
        }
        TraceContext.setTraceId(traceId);
        return traceId;
    }

    /**
     * 消费端：先从载体恢复 traceId，再执行任务，结束后恢复执行前的 MDC。
     * 适用于消费回调、手动包装的监听器等场景。
     *
     * @param carrier 载体读取函数
     * @param task    业务任务
     */
    public static void run(Function<String, String> carrier, Runnable task) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        extract(carrier);
        try {
            task.run();
        } finally {
            TraceContext.restore(previous);
        }
    }

    /**
     * 从载体读取 traceId，去除首尾空白，空白内容视为不存在。
     *
     * @param carrier 载体读取函数
     * @return traceId，不存在时返回 {@code null}
     */
    public static String resolve(Function<String, String> carrier) {
        String traceId = carrier.apply(TraceContext.TRACE_ID);
        if (traceId != null) {
            traceId = traceId.trim();
            if (traceId.isEmpty()) {
                return null;
            }
        }
        return traceId;
    }
}
