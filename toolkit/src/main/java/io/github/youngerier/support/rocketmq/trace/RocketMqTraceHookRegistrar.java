package io.github.youngerier.support.rocketmq.trace;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 为容器中的 RocketMQ 客户端自动注册 trace Hook：
 * {@link DefaultMQProducer} 注册发送 Hook，{@link DefaultMQPushConsumer} 注册消费 Hook。
 * 按客户端 impl 去重，同一 impl（如 RocketMQTemplate 与独立 Producer bean 重合）只注册一次。
 */
public class RocketMqTraceHookRegistrar implements BeanPostProcessor {

    private final ObjectProvider<TraceSendMessageHook> sendHookProvider;
    private final ObjectProvider<TraceConsumeMessageHook> consumeHookProvider;
    private final Set<Object> registered = ConcurrentHashMap.newKeySet();

    public RocketMqTraceHookRegistrar(ObjectProvider<TraceSendMessageHook> sendHookProvider,
                                      ObjectProvider<TraceConsumeMessageHook> consumeHookProvider) {
        this.sendHookProvider = sendHookProvider;
        this.consumeHookProvider = consumeHookProvider;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DefaultMQProducer producer) {
            registerProducer(producer);
        } else if (bean instanceof DefaultMQPushConsumer consumer) {
            registerConsumer(consumer);
        }
        return bean;
    }

    /**
     * 在 producer 上注册发送 Hook，供识别 rocketmq-spring 组件的注册器复用同一去重逻辑。
     */
    public void registerProducer(DefaultMQProducer producer) {
        DefaultMQProducerImpl impl = producer.getDefaultMQProducerImpl();
        if (registered.add(impl)) {
            sendHookProvider.ifAvailable(hook -> impl.registerSendMessageHook(hook));
        }
    }

    /**
     * 在 consumer 上注册消费 Hook，供识别 rocketmq-spring 容器的注册器复用同一去重逻辑。
     */
    public void registerConsumer(DefaultMQPushConsumer consumer) {
        DefaultMQPushConsumerImpl impl = consumer.getDefaultMQPushConsumerImpl();
        if (registered.add(impl)) {
            consumeHookProvider.ifAvailable(hook -> impl.registerConsumeMessageHook(hook));
        }
    }
}
