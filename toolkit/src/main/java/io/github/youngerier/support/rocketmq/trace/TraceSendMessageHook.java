package io.github.youngerier.support.rocketmq.trace;

import io.github.youngerier.support.trace.TraceContext;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.client.hook.SendMessageHook;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RocketMQ 发送端链路 Hook：消息发送前把当前 traceId 写入消息用户属性，
 * 让消费端能从消息中恢复同一 traceId。注册于 {@code DefaultMQProducerImpl}。
 */
public class TraceSendMessageHook implements SendMessageHook {

    private static final Logger log = LoggerFactory.getLogger(TraceSendMessageHook.class);

    @Override
    public String hookName() {
        return "youngerierTraceSendMessageHook";
    }

    @Override
    public void sendMessageBefore(SendMessageContext context) {
        Message message = context.getMessage();
        if (message == null || message.getUserProperty(TraceContext.TRACE_ID) != null) {
            return;
        }
        try {
            message.putUserProperty(TraceContext.TRACE_ID, TraceContext.ensureTraceId());
        } catch (Exception e) {
            // 链路透传不能阻断消息发送
            log.warn("Failed to inject traceId into RocketMQ message, topic={}", message.getTopic(), e);
        }
    }

    @Override
    public void sendMessageAfter(SendMessageContext context) {
        // no-op
    }
}
