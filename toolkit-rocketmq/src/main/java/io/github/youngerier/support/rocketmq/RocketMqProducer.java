package io.github.youngerier.support.rocketmq;

import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * RocketMQ 便捷发送封装：屏蔽 {@code "topic:tag"} destination 拼接、keys/属性头设置与
 * 原生 {@link SendResult} 转换，提供同步 / 异步 / one-way / 顺序 / 延迟等常用发送方式。
 * <p>
 * 常用场景直接调用简短方法，如 {@code producer.sync("order-topic", "created", orderDTO)}；
 * 需要完整控制（超时、延迟、自定义属性）时用 {@link RocketMqMessage} 信封配合
 * {@link #send(RocketMqMessage)}。traceId 由发送 Hook 自动注入，无需手动处理。
 */
public class RocketMqProducer {

    private final RocketMQTemplate template;

    public RocketMqProducer(RocketMQTemplate template) {
        this.template = template;
    }

    // ============================ 消息信封 ============================

    /**
     * 按信封完整配置同步发送，支持超时、延迟级别 / 定时投递、自定义属性。
     */
    public MqSendResult send(RocketMqMessage message) {
        validate(message);
        String destination = destination(message.getTopic(), message.getTag());
        Message<?> springMessage = toSpringMessage(message);

        SendResult result;
        if (message.getDelayTimeMs() != null) {
            result = template.syncSendDelayTimeMills(destination, springMessage, message.getDelayTimeMs());
        } else if (message.getDelayLevel() != null) {
            long timeout = message.getTimeoutMs() != null
                    ? message.getTimeoutMs() : template.getProducer().getSendMsgTimeout();
            result = template.syncSend(destination, springMessage, timeout, message.getDelayLevel());
        } else if (message.getTimeoutMs() != null) {
            result = template.syncSend(destination, springMessage, message.getTimeoutMs());
        } else {
            result = template.syncSend(destination, springMessage);
        }
        return MqSendResult.from(result, message.getTag());
    }

    /**
     * 按信封配置顺序发送：相同 {@code hashKey} 的消息固定进入同一队列、按发送顺序消费。
     */
    public MqSendResult sendOrderly(RocketMqMessage message) {
        validate(message);
        if (!StringUtils.hasText(message.getHashKey())) {
            throw new IllegalArgumentException("RocketMqMessage.hashKey must have text for orderly send");
        }
        // 顺序发送底层只支持 delayLevel，不支持 delayTimeMs 定时消息：显式失败而非静默忽略
        if (message.getDelayTimeMs() != null) {
            throw new IllegalArgumentException(
                    "orderly send does not support delayTimeMs; use delayLevel instead");
        }
        String destination = destination(message.getTopic(), message.getTag());
        Message<?> springMessage = toSpringMessage(message);

        SendResult result;
        if (message.getDelayLevel() != null) {
            long timeout = message.getTimeoutMs() != null
                    ? message.getTimeoutMs() : template.getProducer().getSendMsgTimeout();
            result = template.syncSendOrderly(destination, springMessage, message.getHashKey(),
                    timeout, message.getDelayLevel());
        } else if (message.getTimeoutMs() != null) {
            result = template.syncSendOrderly(destination, springMessage, message.getHashKey(),
                    message.getTimeoutMs());
        } else {
            result = template.syncSendOrderly(destination, springMessage, message.getHashKey());
        }
        return MqSendResult.from(result, message.getTag());
    }

    /**
     * 按信封配置异步发送，发送结果通过回调通知。
     * 支持透传 {@code delayLevel}；异步通道不支持 {@code delayTimeMs} 定时消息（底层 API 缺失），
     * 此时直接 fail-fast，绝不静默降级成立即投递。
     */
    public void async(RocketMqMessage message, MqSendCallback callback) {
        validate(message);
        Objects.requireNonNull(callback, "MqSendCallback must not be null");
        if (message.getDelayTimeMs() != null) {
            throw new IllegalArgumentException(
                    "async send does not support delayTimeMs; use send() for timed messages");
        }
        String destination = destination(message.getTopic(), message.getTag());
        SendCallback adapted = adapt(message.getTag(), callback);
        long timeout = message.getTimeoutMs() != null
                ? message.getTimeoutMs() : template.getProducer().getSendMsgTimeout();
        if (message.getDelayLevel() != null) {
            template.asyncSend(destination, toSpringMessage(message), adapted,
                    timeout, message.getDelayLevel());
        } else if (message.getTimeoutMs() != null) {
            template.asyncSend(destination, toSpringMessage(message), adapted, timeout);
        } else {
            template.asyncSend(destination, toSpringMessage(message), adapted);
        }
    }

    // ============================ 同步发送 ============================

    /**
     * 同步发送普通消息。
     */
    public MqSendResult sync(String topic, Object payload) {
        return sync(topic, null, payload, null);
    }

    /**
     * 同步发送带 tag 的消息。
     */
    public MqSendResult sync(String topic, String tag, Object payload) {
        return sync(topic, tag, payload, null);
    }

    /**
     * 同步发送带 tag 和业务 keys 的消息。
     */
    public MqSendResult sync(String topic, String tag, Object payload, String keys) {
        validate(topic, payload);
        SendResult result = template.syncSend(destination(topic, tag), build(payload, keys));
        return MqSendResult.from(result, tag);
    }

    // ============================ 延迟 / 定时发送 ============================

    /**
     * 按延迟级别发送（RocketMQ 内置 1-18 个固定级别）。
     */
    public MqSendResult delayLevel(String topic, Object payload, int delayLevel) {
        return delayLevel(topic, null, payload, delayLevel);
    }

    /**
     * 按延迟级别发送带 tag 的消息。
     */
    public MqSendResult delayLevel(String topic, String tag, Object payload, int delayLevel) {
        validate(topic, payload);
        long timeout = template.getProducer().getSendMsgTimeout();
        SendResult result = template.syncSend(destination(topic, tag), build(payload, null),
                timeout, delayLevel);
        return MqSendResult.from(result, tag);
    }

    /**
     * 定时发送：在 {@code delay} 之后投递，需 Broker 5.0 及以上。
     */
    public MqSendResult delay(String topic, Object payload, Duration delay) {
        return delay(topic, null, payload, delay);
    }

    /**
     * 定时发送带 tag 的消息：在 {@code delay} 之后投递，需 Broker 5.0 及以上。
     */
    public MqSendResult delay(String topic, String tag, Object payload, Duration delay) {
        validate(topic, payload);
        if (delay == null || delay.isNegative() || delay.isZero()) {
            throw new IllegalArgumentException("delay must be a positive Duration");
        }
        SendResult result = template.syncSendDelayTimeMills(destination(topic, tag),
                build(payload, null), delay.toMillis());
        return MqSendResult.from(result, tag);
    }

    // ============================ 异步发送 ============================

    /**
     * 异步发送，不阻塞当前线程。
     */
    public void async(String topic, Object payload, MqSendCallback callback) {
        async(topic, null, payload, callback);
    }

    /**
     * 异步发送带 tag 的消息。
     */
    public void async(String topic, String tag, Object payload, MqSendCallback callback) {
        validate(topic, payload);
        Objects.requireNonNull(callback, "MqSendCallback must not be null");
        template.asyncSend(destination(topic, tag), build(payload, null), adapt(tag, callback));
    }

    // ============================ One-Way ============================

    /**
     * 单向发送：不等待 Broker 确认、无重试，适用于日志采集等可丢失场景。
     */
    public void oneWay(String topic, Object payload) {
        oneWay(topic, null, payload);
    }

    /**
     * 单向发送带 tag 的消息。
     */
    public void oneWay(String topic, String tag, Object payload) {
        validate(topic, payload);
        template.sendOneWay(destination(topic, tag), build(payload, null));
    }

    // ============================ 顺序消息 ============================

    /**
     * 同步顺序发送：相同 hashKey 的消息进入同一队列。
     */
    public MqSendResult syncOrderly(String topic, Object payload, String hashKey) {
        return syncOrderly(topic, null, payload, hashKey);
    }

    /**
     * 同步顺序发送带 tag 的消息。
     */
    public MqSendResult syncOrderly(String topic, String tag, Object payload, String hashKey) {
        validate(topic, payload);
        if (!StringUtils.hasText(hashKey)) {
            throw new IllegalArgumentException("hashKey must have text for orderly send");
        }
        SendResult result = template.syncSendOrderly(destination(topic, tag),
                build(payload, null), hashKey);
        return MqSendResult.from(result, tag);
    }

    /**
     * 单向顺序发送：相同 hashKey 的消息进入同一队列，不等待确认。
     */
    public void oneWayOrderly(String topic, Object payload, String hashKey) {
        oneWayOrderly(topic, null, payload, hashKey);
    }

    /**
     * 单向顺序发送带 tag 的消息。
     */
    public void oneWayOrderly(String topic, String tag, Object payload, String hashKey) {
        validate(topic, payload);
        if (!StringUtils.hasText(hashKey)) {
            throw new IllegalArgumentException("hashKey must have text for orderly send");
        }
        template.sendOneWayOrderly(destination(topic, tag), build(payload, null), hashKey);
    }

    // ============================ 内部工具 ============================

    private static Message<?> toSpringMessage(RocketMqMessage message) {
        MessageBuilder<?> builder = MessageBuilder.withPayload(message.getPayload());
        if (StringUtils.hasText(message.getKeys())) {
            builder.setHeader(RocketMQHeaders.KEYS, message.getKeys());
        }
        Map<String, String> properties = message.getProperties();
        if (properties != null) {
            properties.forEach((name, value) -> {
                if (name != null && value != null) {
                    builder.setHeader(name, value);
                }
            });
        }
        return builder.build();
    }

    private static Message<?> build(Object payload, String keys) {
        MessageBuilder<?> builder = MessageBuilder.withPayload(payload);
        if (StringUtils.hasText(keys)) {
            builder.setHeader(RocketMQHeaders.KEYS, keys);
        }
        return builder.build();
    }

    private static String destination(String topic, String tag) {
        return StringUtils.hasText(tag) ? topic + ":" + tag : topic;
    }

    private static void validate(RocketMqMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("RocketMqMessage must not be null");
        }
        validate(message.getTopic(), message.getPayload());
    }

    private static void validate(String topic, Object payload) {
        if (!StringUtils.hasText(topic)) {
            throw new IllegalArgumentException("topic must have text");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
    }

    private static SendCallback adapt(String tag, MqSendCallback callback) {
        return new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                callback.onSuccess(MqSendResult.from(sendResult, tag));
            }

            @Override
            public void onException(Throwable exception) {
                callback.onException(exception);
            }
        };
    }
}
