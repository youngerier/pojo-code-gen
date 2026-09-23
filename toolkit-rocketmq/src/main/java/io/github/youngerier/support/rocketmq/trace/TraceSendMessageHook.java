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
            // 只读取当前线程的 traceId，缺失时就地生成、不写回 MDC：
            // 本 Hook 在业务调用线程上执行，写 MDC 会让线程池复用时
            // 该线程后续发送的所有消息共用同一个 traceId
            String traceId = TraceContext.getTraceId();
            if (traceId == null) {
                traceId = TraceContext.generateTraceId();
            }
            message.putUserProperty(TraceContext.TRACE_ID, traceId);
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
