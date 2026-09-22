package io.github.youngerier.support.rocketmq;

import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;

/**
 * 精简的消息发送结果：只保留业务排查常用字段，屏蔽 RocketMQ 原生 {@link SendResult} 的冗余信息。
 *
 * @param topic      实际投递的 topic
 * @param tag        消息 tag，未指定时为 {@code null}
 * @param msgId      消息 ID
 * @param queueId    投递到的队列编号
 * @param sendStatus 发送状态
 */
public record MqSendResult(String topic, String tag, String msgId, int queueId, SendStatus sendStatus) {

    /**
     * 由 RocketMQ 原生结果转换，tag 取自发送时的入参（原生结果不含 tag）。
     */
    public static MqSendResult from(SendResult result, String tag) {
        return new MqSendResult(result.getMessageQueue().getTopic(), tag, result.getMsgId(),
                result.getMessageQueue().getQueueId(), result.getSendStatus());
    }

    /**
     * 是否发送成功（Broker 存储确认）。
     */
    public boolean isSuccess() {
        return sendStatus == SendStatus.SEND_OK;
    }
}
