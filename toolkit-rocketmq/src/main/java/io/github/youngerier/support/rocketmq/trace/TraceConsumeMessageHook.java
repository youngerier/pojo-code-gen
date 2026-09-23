package io.github.youngerier.support.rocketmq.trace;

import io.github.youngerier.support.trace.TraceContext;
import io.github.youngerier.support.trace.TracePropagator;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;
import java.util.Map;

/**
 * RocketMQ 消费端链路 Hook：监听器执行前从本批消息中恢复 traceId 到 MDC，
 * 执行结束后把 MDC 还原到执行前的状态。注册于 {@code DefaultMQPushConsumerImpl}。
 */
public class TraceConsumeMessageHook implements ConsumeMessageHook {

    /**
     * 消费线程的 MDC 快照。{@code consumeMessageBefore}/{@code consumeMessageAfter}
     * 由 RocketMQ 在同一消费线程上成对调用，因此用 ThreadLocal 传递快照。
     */
    private final ThreadLocal<Map<String, String>> previousContext = new ThreadLocal<>();

    @Override
    public String hookName() {
        return "youngerierTraceConsumeMessageHook";
    }

    @Override
    public void consumeMessageBefore(ConsumeMessageContext context) {
        previousContext.set(TraceContext.snapshot());
        String traceId = null;
        List<MessageExt> messages = context.getMsgList();
        if (messages != null && !messages.isEmpty()) {
            // getUserProperty 对空 properties 安全（无任何属性的消息 properties 为 null）；
            // 上游属性会经过 TracePropagator.resolve 的白名单校验
            traceId = TracePropagator.resolve(messages.get(0)::getUserProperty);
        }
        if (traceId == null) {
            traceId = TraceContext.generateTraceId();
        }
        TraceContext.setTraceId(traceId);
    }

    @Override
    public void consumeMessageAfter(ConsumeMessageContext context) {
        Map<String, String> previous = previousContext.get();
        previousContext.remove();
        // 消费线程池会复用，必须还原（而非只删 traceId），
        // 避免 traceId 与其他业务 MDC 键串到下一条消息
        TraceContext.restore(previous);
    }
}
