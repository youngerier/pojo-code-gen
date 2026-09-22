package io.github.youngerier.support.rocketmq;

/**
 * 异步发送回调，替代直接依赖 RocketMQ 的 {@code SendCallback}。
 */
public interface MqSendCallback {

    /**
     * 消息发送成功。
     */
    void onSuccess(MqSendResult result);

    /**
     * 消息发送失败（网络异常、Broker 拒绝等）。
     */
    void onException(Throwable exception);
}
