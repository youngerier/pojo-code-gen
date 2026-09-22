package io.github.youngerier.support.rocketmq.consumer;

import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractRocketMqListenerTest {

    private static class TestListener extends AbstractRocketMqListener<String> {

        private Exception toThrow;
        private String consumed;

        @Override
        protected void consume(String message) throws Exception {
            consumed = message;
            if (toThrow != null) {
                throw toThrow;
            }
        }
    }

    private static class MessageExtListener extends AbstractRocketMqListener<MessageExt> {

        @Override
        protected void consume(MessageExt message) {
            // 泛型为 MessageExt 时直接拿到元数据
        }
    }

    @Test
    void consumesPayloadSuccessfully() {
        TestListener listener = new TestListener();

        listener.onMessage("hello");

        assertEquals("hello", listener.consumed);
    }

    @Test
    void runtimeExceptionRethrownAsIs() {
        TestListener listener = new TestListener();
        IllegalStateException failure = new IllegalStateException("boom");
        listener.toThrow = failure;

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> listener.onMessage("hello"));

        assertSame(failure, thrown);
    }

    @Test
    void checkedExceptionWrappedToTriggerRetry() {
        TestListener listener = new TestListener();
        Exception failure = new Exception("checked boom");
        listener.toThrow = failure;

        RocketMqConsumeException thrown = assertThrows(RocketMqConsumeException.class,
                () -> listener.onMessage("hello"));

        assertSame(failure, thrown.getCause());
    }

    @Test
    void acceptsMessageExtPayload() {
        MessageExt messageExt = new MessageExt();
        messageExt.setMsgId("msg-1");
        messageExt.setKeys("key-1");

        new MessageExtListener().onMessage(messageExt);
    }
}
