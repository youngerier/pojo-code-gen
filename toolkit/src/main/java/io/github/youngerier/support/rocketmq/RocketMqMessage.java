package io.github.youngerier.support.rocketmq;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * RocketMQ 消息信封：承载 topic、tag、业务 payload 及 keys / 超时 / 延迟 / 顺序等发送选项。
 * 不可变值对象，配合 {@link RocketMqProducer#send(RocketMqMessage)} 使用，
 * 常用场景则直接调用 {@link RocketMqProducer} 的简短方法。
 */
@Getter
@Builder
public class RocketMqMessage {

    /**
     * 目标 topic，必填。
     */
    private final String topic;

    /**
     * 消息 tag，可空，用于消费端过滤。
     */
    private final String tag;

    /**
     * 消息业务 keys，可空，多个用英文逗号分隔，Broker 按 key 建立哈希索引便于查询。
     */
    private final String keys;

    /**
     * 业务消息体，必填；非 String/byte[] 时由 RocketMQ 消息转换器序列化为 JSON。
     */
    private final Object payload;

    /**
     * 发送超时（毫秒），可空，未指定时使用 Producer 默认值。
     */
    private final Long timeoutMs;

    /**
     * 延迟级别（1-18），可空。与 {@link #delayTimeMs} 同时设置时后者优先（需 Broker 5.x）。
     */
    private final Integer delayLevel;

    /**
     * 定时投递的目标时间戳（毫秒），可空，需 Broker 5.0 及以上。
     */
    private final Long delayTimeMs;

    /**
     * 顺序消息的分区 hashKey，可空；相同 hashKey 的消息固定路由到同一队列。
     */
    private final String hashKey;

    /**
     * 额外的消息用户属性，可空，随消息跨进程传递。
     */
    private final Map<String, String> properties;
}
