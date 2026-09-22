package io.github.youngerier.support.rocketmq.trace;

import io.github.youngerier.support.trace.TraceContext;
import io.github.youngerier.support.trace.TracePropagator;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * RocketMQ 消费端链路 Hook：监听器执行前从本批消息中恢复 traceId 到 MDC，
 * 执行结束后清理 MDC。注册于 {@code DefaultMQPushConsumerImpl}。
 */
public class TraceConsumeMessageHook implements ConsumeMessageHook {

    @Override
    public String hookName() {
        return "youngerierTraceConsumeMessageHook";
    }

    @Override
    public void consumeMessageBefore(ConsumeMessageContext context) {
        String traceId = null;
        List<MessageExt> messages = context.getMsgList();
        if (messages != null && !messages.isEmpty()) {
            // getUserProperty 对空 properties 安全（无任何属性的消息 properties 为 null）
            traceId = TracePropagator.resolve(messages.get(0)::getUserProperty);
        }
        if (traceId == null) {
            traceId = TraceContext.generateTraceId();
        }
        TraceContext.setTraceId(traceId);
    }

    @Override
    public void consumeMessageAfter(ConsumeMessageContext context) {
        // 消费线程池会复用，必须清理，避免 traceId 串到下一条消息
        TraceContext.clear();
    }
}
