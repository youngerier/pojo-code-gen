package io.github.youngerier.support.rocketmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RocketMqProducerTest {

    private RocketMQTemplate template;
    private RocketMqProducer producer;

    private static SendResult stubResult(String topic) {
        SendResult result = new SendResult();
        result.setMessageQueue(new MessageQueue(topic, "broker-a", 3));
        result.setMsgId("msg-1");
        result.setSendStatus(SendStatus.SEND_OK);
        return result;
    }

    @BeforeEach
    void setUp() {
        template = mock(RocketMQTemplate.class);
        DefaultMQProducer mqProducer = mock(DefaultMQProducer.class);
        when(mqProducer.getSendMsgTimeout()).thenReturn(3000);
        when(template.getProducer()).thenReturn(mqProducer);
        producer = new RocketMqProducer(template);
    }

    @Test
    void syncSendsWithTopicAndTag() {
        Object payload = Map.of("id", 1);
        when(template.syncSend(eq("order-topic:created"), any(Message.class)))
                .thenReturn(stubResult("order-topic"));

        MqSendResult result = producer.sync("order-topic", "created", payload);

        assertEquals("msg-1", result.msgId());
        assertEquals(3, result.queueId());
        assertEquals("created", result.tag());
        assertTrue(result.isSuccess());
    }

    @Test
    void syncWithoutTagUsesBareTopic() {
        when(template.syncSend(eq("order-topic"), any(Message.class)))
                .thenReturn(stubResult("order-topic"));

        producer.sync("order-topic", "payload");

        verify(template).syncSend(eq("order-topic"), any(Message.class));
    }

    @Test
    void syncSetsKeysHeader() {
        when(template.syncSend(eq("order-topic"), any(Message.class)))
                .thenReturn(stubResult("order-topic"));

        producer.sync("order-topic", null, "payload", "order-1");

        ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
        verify(template).syncSend(eq("order-topic"), captor.capture());
        assertEquals("order-1", captor.getValue().getHeaders().get("KEYS"));
    }

    @Test
    void sendEnvelopeAppliesHeadersTimeoutAndProperties() {
        when(template.syncSend(eq("order-topic:created"), any(Message.class), eq(2000L)))
                .thenReturn(stubResult("order-topic"));
        RocketMqMessage envelope = RocketMqMessage.builder()
                .topic("order-topic").tag("created").payload("payload")
                .keys("order-1").timeoutMs(2000L)
                .properties(Map.of("bizId", "42")).build();

        MqSendResult result = producer.send(envelope);

        ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
        verify(template).syncSend(eq("order-topic:created"), captor.capture(), eq(2000L));
        Message<?> message = captor.getValue();
        assertEquals("order-1", message.getHeaders().get("KEYS"));
        assertEquals("42", message.getHeaders().get("bizId"));
        assertEquals("msg-1", result.msgId());
    }

    @Test
    void sendEnvelopeWithTimerUsesDelayTime() {
        when(template.syncSendDelayTimeMills(eq("order-topic"), any(Message.class), eq(60000L)))
                .thenReturn(stubResult("order-topic"));
        RocketMqMessage envelope = RocketMqMessage.builder()
                .topic("order-topic").payload("payload").delayTimeMs(60000L).build();

        producer.send(envelope);

        verify(template).syncSendDelayTimeMills(eq("order-topic"), any(Message.class), eq(60000L));
    }

    @Test
    void delayLevelDispatchesWithLevel() {
        when(template.syncSend(eq("order-topic"), any(Message.class), eq(3000L), eq(3)))
                .thenReturn(stubResult("order-topic"));

        producer.delayLevel("order-topic", "payload", 3);

        verify(template).syncSend(eq("order-topic"), any(Message.class), eq(3000L), eq(3));
    }

    @Test
    void delayDurationDispatchesTimer() {
        when(template.syncSendDelayTimeMills(eq("order-topic"), any(Message.class), eq(60000L)))
                .thenReturn(stubResult("order-topic"));

        producer.delay("order-topic", "payload", Duration.ofMinutes(1));

        verify(template).syncSendDelayTimeMills(eq("order-topic"), any(Message.class), eq(60000L));
    }

    @Test
    void asyncAdaptsCallbackBothPaths() {
        MqSendCallback callback = mock(MqSendCallback.class);
        producer.async("order-topic", "created", "payload", callback);

        ArgumentCaptor<SendCallback> captor = ArgumentCaptor.forClass(SendCallback.class);
        verify(template).asyncSend(eq("order-topic:created"), any(Message.class), captor.capture());

        captor.getValue().onSuccess(stubResult("order-topic"));
        verify(callback).onSuccess(any(MqSendResult.class));

        RuntimeException failure = new RuntimeException("broker down");
        captor.getValue().onException(failure);
        verify(callback).onException(failure);
    }

    @Test
    void oneWayDispatchesWithoutResult() {
        producer.oneWay("log-topic", "event");
        verify(template).sendOneWay(eq("log-topic"), any(Message.class));
    }

    @Test
    void orderlySendsUseHashKey() {
        when(template.syncSendOrderly(eq("order-topic:created"), any(Message.class), eq("order-1")))
                .thenReturn(stubResult("order-topic"));

        producer.syncOrderly("order-topic", "created", "payload", "order-1");

        verify(template).syncSendOrderly(eq("order-topic:created"), any(Message.class), eq("order-1"));
    }

    @Test
    void oneWayOrderlyDispatches() {
        producer.oneWayOrderly("order-topic", "payload", "order-1");
        verify(template).sendOneWayOrderly(eq("order-topic"), any(Message.class), eq("order-1"));
    }

    @Test
    void sendOrderlyEnvelopeRegistersHookPath() {
        when(template.syncSendOrderly(eq("order-topic"), any(Message.class), eq("order-1")))
                .thenReturn(stubResult("order-topic"));
        RocketMqMessage envelope = RocketMqMessage.builder()
                .topic("order-topic").payload("payload").hashKey("order-1").build();

        producer.sendOrderly(envelope);

        verify(template).syncSendOrderly(eq("order-topic"), any(Message.class), eq("order-1"));
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> producer.sync(" ", "payload"));
        assertThrows(IllegalArgumentException.class, () -> producer.sync("topic", null));
        assertThrows(IllegalArgumentException.class, () -> producer.syncOrderly("topic", "payload", " "));
        assertThrows(IllegalArgumentException.class, () -> producer.delay("topic", "payload", Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> producer.send(RocketMqMessage.builder().topic("topic").build()));
        assertThrows(IllegalArgumentException.class,
                () -> producer.sendOrderly(RocketMqMessage.builder().topic("topic").payload("p").build()));
    }
}
