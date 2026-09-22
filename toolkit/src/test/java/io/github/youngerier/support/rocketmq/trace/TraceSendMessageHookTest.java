package io.github.youngerier.support.rocketmq.trace;

import io.github.youngerier.support.trace.TraceContext;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void generatesTraceIdWhenMdcEmpty() {
        Message message = new Message("test-topic", "hello".getBytes());

        hook.sendMessageBefore(contextWith(message));

        String traceId = message.getUserProperty(TraceContext.TRACE_ID);
        assertEquals(32, traceId.length());
        assertEquals(traceId, TraceContext.getTraceId());
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
