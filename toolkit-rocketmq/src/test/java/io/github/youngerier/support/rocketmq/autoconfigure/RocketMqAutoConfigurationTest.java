package io.github.youngerier.support.rocketmq.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归：classpath 有 rocketmq-spring（本模块必需依赖）、但容器中没有
 * {@code RocketMQTemplate} Bean 时，上下文必须能正常启动，且不得注册 {@code RocketMqProducer}。
 *
 * <p>对应缺陷：只判断 {@code @ConditionalOnClass(RocketMQTemplate.class)} 时，
 * rocketmq-spring 因缺少 {@code rocketmq.name-server} 会跳过全部 bean（含 {@code RocketMQTemplate}），
 * 而本模块仍尝试注入它，导致使用方应用启动失败（NoSuchBeanDefinitionException）。
 */
class RocketMqAutoConfigurationTest {

    @Test
    void rocketMqProducerSkippedWhenNoRocketMQTemplateBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RocketMqAutoConfiguration.class);
            assertDoesNotThrow(context::refresh);

            assertFalse(context.containsBean("rocketMqProducer"),
                    "容器中没有 RocketMQTemplate Bean 时不得装配 RocketMqProducer");
            // 链路 Hook 仍然应当装配：它们只依赖 rocketmq-client，不依赖 RocketMQTemplate
            assertTrue(context.containsBean("traceSendMessageHook"));
            assertTrue(context.containsBean("traceConsumeMessageHook"));
        }
    }

    /**
     * 开关：{@code youngerier.rocketmq.enabled=false} 时整体不装配。
     */
    @Test
    void canBeDisabledByProperty() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new org.springframework.core.env.MapPropertySource("test",
                            java.util.Map.of("youngerier.rocketmq.enabled", "false")));
            context.register(RocketMqAutoConfiguration.class);
            assertDoesNotThrow(context::refresh);

            assertFalse(context.containsBean("traceSendMessageHook"));
            assertFalse(context.containsBean("traceConsumeMessageHook"));
        }
    }
}
