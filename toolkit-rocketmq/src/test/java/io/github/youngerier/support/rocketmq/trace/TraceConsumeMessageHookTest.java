package io.github.youngerier.support.rocketmq.trace;

import io.github.youngerier.support.trace.TraceContext;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraceConsumeMessageHookTest {

    private final TraceConsumeMessageHook hook = new TraceConsumeMessageHook();

    @AfterEach
    void clearMdc() {
        // 整体清空：本类会写入业务键，必须完全隔离
        MDC.clear();
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

    /**
     * 回归：消息属性里的 traceId 来自上游，可能被伪造或携带换行/控制字符（日志伪造），
     * 非法值必须被丢弃并重新生成。
     */
    @Test
    void rejectsIllegalTraceIdProperty() {
        MessageExt message = new MessageExt();
        message.putUserProperty(TraceContext.TRACE_ID, "bad-id\r\nINFO forged log line");
        ConsumeMessageContext context = new ConsumeMessageContext();
        context.setMsgList(List.of(message));

        hook.consumeMessageBefore(context);

        String traceId = TraceContext.getTraceId();
        assertEquals(32, traceId.length());
        assertNotEquals("bad-id\r\nINFO forged log line", traceId);
    }

    /**
     * 回归：消费结束后必须把 MDC 还原到消费前的状态，而不是只删 traceId，
     * 否则业务/框架写入的其它 MDC 键会串到下一条复用该线程的消息。
     */
    @Test
    void restoresOtherMdcKeysAfterConsume() {
        MDC.put("bizKey", "biz-value");
        MessageExt message = new MessageExt();
        ConsumeMessageContext context = new ConsumeMessageContext();
        context.setMsgList(List.of(message));

        hook.consumeMessageBefore(context);
        assertEquals("biz-value", MDC.get("bizKey"));

        hook.consumeMessageAfter(context);
        assertEquals("biz-value", MDC.get("bizKey"));
        assertNull(TraceContext.getTraceId());
    }
}
