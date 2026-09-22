package io.github.youngerier.support.rocketmq.trace;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
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
    @SuppressWarnings("deprecation")
    public void registerProducer(DefaultMQProducer producer) {
        // DefaultMQProducer 未公开 SendMessageHook 注册入口，只能取内部 impl；
        // getDefaultMQProducerImpl() 虽标记过时，但其官方替代 API 至今不存在，
        // rocketmq-spring 自身（RocketMQUtil）也是这样注册 Hook 的。
        DefaultMQProducerImpl impl = producer.getDefaultMQProducerImpl();
        if (registered.add(impl)) {
            sendHookProvider.ifAvailable(hook -> impl.registerSendMessageHook(hook));
        }
    }

    /**
     * 在 consumer 上注册消费 Hook，供识别 rocketmq-spring 容器的注册器复用同一去重逻辑。
     */
    public void registerConsumer(DefaultMQPushConsumer consumer) {
        if (registered.add(consumer)) {
            consumeHookProvider.ifAvailable(consumer::registerConsumeMessageHook);
        }
    }
}
