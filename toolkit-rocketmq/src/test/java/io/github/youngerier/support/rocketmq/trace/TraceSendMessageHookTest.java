package io.github.youngerier.support.rocketmq.trace;

import io.github.youngerier.support.trace.TraceContext;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraceSendMessageHookTest {

    private final TraceSendMessageHook hook = new TraceSendMessageHook();

    @AfterEach
    void clearMdc() {
        TraceContext.clear();
    }

    private SendMessageContext contextWith(Message message) {
        SendMessageContext context = new SendMessageContext();
        context.setMessage(message);
        return context;
    }

    @Test
    void injectsCurrentTraceId() {
        TraceContext.setTraceId("producer-trace-id");
        Message message = new Message("test-topic", "hello".getBytes());

        hook.sendMessageBefore(contextWith(message));

        assertEquals("producer-trace-id", message.getUserProperty(TraceContext.TRACE_ID));
    }

    @Test
    void generatesTraceIdWithoutPollutingCallerMdc() {
        Message message = new Message("test-topic", "hello".getBytes());

        hook.sendMessageBefore(contextWith(message));

        String traceId = message.getUserProperty(TraceContext.TRACE_ID);
        assertEquals(32, traceId.length());
        // 不得把新生成的 traceId 写回调用线程 MDC：本 Hook 在业务调用线程上执行，
        // 一旦写回，线程池复用时该线程后续发送的所有消息都会共用同一个 traceId
        assertNull(TraceContext.getTraceId());
    }

    @Test
    void doesNotOverrideExistingTraceId() {
        TraceContext.setTraceId("producer-trace-id");
        Message message = new Message("test-topic", "hello".getBytes());
        message.putUserProperty(TraceContext.TRACE_ID, "already-set");

        hook.sendMessageBefore(contextWith(message));

        assertEquals("already-set", message.getUserProperty(TraceContext.TRACE_ID));
    }

    @Test
    void toleratesNullMessage() {
        assertDoesNotThrow(() -> hook.sendMessageBefore(contextWith(null)));
    }
}
