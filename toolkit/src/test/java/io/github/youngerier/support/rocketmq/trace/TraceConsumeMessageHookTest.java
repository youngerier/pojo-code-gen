package io.github.youngerier.support.rocketmq.trace;

import io.github.youngerier.support.trace.TraceContext;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraceConsumeMessageHookTest {

    private final TraceConsumeMessageHook hook = new TraceConsumeMessageHook();

    @AfterEach
    void clearMdc() {
        TraceContext.clear();
    }

    @Test
    void restoresTraceIdFromMessageAndClearsAfter() {
        MessageExt message = new MessageExt();
        message.setTopic("test-topic");
        message.setBody("hello".getBytes());
        message.putUserProperty(TraceContext.TRACE_ID, "producer-trace-id");
        ConsumeMessageContext context = new ConsumeMessageContext();
        context.setMsgList(List.of(message));

        hook.consumeMessageBefore(context);
        assertEquals("producer-trace-id", TraceContext.getTraceId());

        hook.consumeMessageAfter(context);
        assertNull(TraceContext.getTraceId());
    }

    @Test
    void generatesTraceIdWhenPropertyMissing() {
        MessageExt message = new MessageExt();
        ConsumeMessageContext context = new ConsumeMessageContext();
        context.setMsgList(List.of(message));

        hook.consumeMessageBefore(context);

        assertEquals(32, TraceContext.getTraceId().length());
    }

    @Test
    void generatesTraceIdWhenMsgListEmpty() {
        ConsumeMessageContext context = new ConsumeMessageContext();

        hook.consumeMessageBefore(context);

        assertEquals(32, TraceContext.getTraceId().length());
    }
}
