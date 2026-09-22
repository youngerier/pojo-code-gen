package io.github.youngerier.support.rocketmq.trace;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * rocketmq-spring 扩展点注册器：{@link RocketMQTemplate} 注册发送 Hook，
 * {@link DefaultRocketMQListenerContainer}（@RocketMQMessageListener 容器）注册消费 Hook。
 * 仅 classpath 存在 rocketmq-spring 时才会装配，委托 {@link RocketMqTraceHookRegistrar} 去重。
 */
public class RocketMqSpringTraceHookRegistrar implements BeanPostProcessor {

    private final RocketMqTraceHookRegistrar delegate;

    public RocketMqSpringTraceHookRegistrar(RocketMqTraceHookRegistrar delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof RocketMQTemplate template) {
            delegate.registerProducer(template.getProducer());
        } else if (bean instanceof DefaultRocketMQListenerContainer container) {
            // afterPropertiesSet() 已完成 consumer 初始化，SmartLifecycle start 尚未触发
            DefaultMQPushConsumer consumer = container.getConsumer();
            if (consumer != null) {
                delegate.registerConsumer(consumer);
            }
        }
        return bean;
    }
}
