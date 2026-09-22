package io.github.youngerier.support.rocketmq.consumer;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RocketMQ 消费端基类：统一消费入口日志、耗时统计与异常处理。
 * 子类只需实现 {@link #consume(Object)}，traceId 已由消费 Hook 恢复到 MDC，日志自动与生产端串联。
 * <p>
 * 需要消息元数据（msgId、keys、重试次数）时，泛型可直接使用 {@link MessageExt}。
 *
 * @param <T> 消息体类型
 */
public abstract class AbstractRocketMqListener<T> implements RocketMQListener<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public final void onMessage(T message) {
        long start = System.currentTimeMillis();
        if (message instanceof MessageExt messageExt) {
            log.info("RocketMQ message received, msgId={}, keys={}, reconsumeTimes={}",
                    messageExt.getMsgId(), messageExt.getKeys(), messageExt.getReconsumeTimes());
        }
        try {
            consume(message);
            log.info("RocketMQ message consumed successfully, cost={}ms",
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("RocketMQ message consume failed, cost={}ms",
                    System.currentTimeMillis() - start, e);
            if (e instanceof RuntimeException runtimeException) {
                // 抛出后由 RocketMQ 按消费策略重试 / 进入死信队列
                throw runtimeException;
            }
            throw new RocketMqConsumeException("RocketMQ message consume failed: " + e.getMessage(), e);
        }
    }

    /**
     * 业务消费逻辑，抛出异常会触发 RocketMQ 重试。
     */
    protected abstract void consume(T message) throws Exception;
}
