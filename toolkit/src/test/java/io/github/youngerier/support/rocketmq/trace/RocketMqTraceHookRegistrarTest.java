package io.github.youngerier.support.rocketmq.trace;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RocketMqTraceHookRegistrarTest {

    private static final String SEND_HOOK_FIELD = "sendMessageHookList";
    private static final String CONSUME_HOOK_FIELD = "consumeMessageHookList";

    @SuppressWarnings("unchecked")
    private static <T> List<T> readField(Object target, String fieldName, Class<?> type) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (List<T>) field.get(target);
    }

    @Test
    void registersHooksOnClientBeansAndDeduplicates() throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("test-producer-group");
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("test-consumer-group");

        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean(TraceSendMessageHook.class);
            ctx.registerBean(TraceConsumeMessageHook.class);
            ctx.registerBean(RocketMqTraceHookRegistrar.class);
            ctx.registerBean("producer", DefaultMQProducer.class, () -> producer);
            ctx.registerBean("consumer", DefaultMQPushConsumer.class, () -> consumer);
            ctx.refresh();

            DefaultMQProducerImpl producerImpl = producer.getDefaultMQProducerImpl();
            DefaultMQPushConsumerImpl consumerImpl = consumer.getDefaultMQPushConsumerImpl();

            List<TraceSendMessageHook> sendHooks =
                    readField(producerImpl, SEND_HOOK_FIELD, DefaultMQProducerImpl.class);
            List<TraceConsumeMessageHook> consumeHooks =
                    readField(consumerImpl, CONSUME_HOOK_FIELD, DefaultMQPushConsumerImpl.class);
            assertEquals(1, sendHooks.size());
            assertEquals(1, consumeHooks.size());

            RocketMqTraceHookRegistrar registrar = ctx.getBean(RocketMqTraceHookRegistrar.class);
            registrar.postProcessAfterInitialization(producer, "producer");
            registrar.postProcessAfterInitialization(consumer, "consumer");
            assertEquals(1, sendHooks.size());
            assertEquals(1, consumeHooks.size());
        }
    }

    @Test
    void registersHooksOnRocketMqSpringComponents() throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("test-producer-group");
        DefaultMQPushConsumer containerConsumer = new DefaultMQPushConsumer("test-consumer-group");
        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(producer);
        DefaultRocketMQListenerContainer container = new DefaultRocketMQListenerContainer();
        container.setConsumer(containerConsumer);

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("sendHook", new TraceSendMessageHook());
        beanFactory.registerSingleton("consumeHook", new TraceConsumeMessageHook());
        RocketMqTraceHookRegistrar delegate =
                new RocketMqTraceHookRegistrar(beanFactory.getBeanProvider(TraceSendMessageHook.class),
                        beanFactory.getBeanProvider(TraceConsumeMessageHook.class));
        RocketMqSpringTraceHookRegistrar springRegistrar = new RocketMqSpringTraceHookRegistrar(delegate);

        // 与独立 producer bean 同一 impl，去重
        delegate.registerProducer(producer);
        springRegistrar.postProcessAfterInitialization(template, "rocketMQTemplate");
        springRegistrar.postProcessAfterInitialization(container, "container");
        springRegistrar.postProcessAfterInitialization(container, "container");

        List<TraceSendMessageHook> sendHooks =
                readField(producer.getDefaultMQProducerImpl(), SEND_HOOK_FIELD, DefaultMQProducerImpl.class);
        List<TraceConsumeMessageHook> consumeHooks =
                readField(containerConsumer.getDefaultMQPushConsumerImpl(), CONSUME_HOOK_FIELD,
                        DefaultMQPushConsumerImpl.class);
        assertEquals(1, sendHooks.size());
        assertEquals(1, consumeHooks.size());
    }
}
