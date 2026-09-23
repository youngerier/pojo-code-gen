package io.github.youngerier.support.rocketmq.consumer;

/**
 * 消费业务异常：消费端基类把受检异常包装为本异常抛出，RocketMQ 据此判定消费失败并按策略重试。
 */
public class RocketMqConsumeException extends RuntimeException {

    public RocketMqConsumeException(String message, Throwable cause) {
        super(message, cause);
    }
}
