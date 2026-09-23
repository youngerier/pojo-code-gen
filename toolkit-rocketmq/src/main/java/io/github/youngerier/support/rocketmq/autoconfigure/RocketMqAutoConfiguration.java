package io.github.youngerier.support.rocketmq.autoconfigure;

import io.github.youngerier.support.rocketmq.RocketMqProducer;
import io.github.youngerier.support.rocketmq.trace.RocketMqSpringTraceHookRegistrar;
import io.github.youngerier.support.rocketmq.trace.RocketMqTraceHookRegistrar;
import io.github.youngerier.support.rocketmq.trace.TraceConsumeMessageHook;
import io.github.youngerier.support.rocketmq.trace.TraceSendMessageHook;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.client.hook.SendMessageHook;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 基础能力自动装配：
 * 链路透传 Hook（发送前注入 traceId、消费前恢复 traceId）与便捷发送封装 {@link RocketMqProducer}。
 * 仅 classpath 存在 rocketmq-client 且未显式关闭（youngerier.rocketmq.enabled=false）时生效。
 */
@AutoConfiguration(afterName = "org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration")
@ConditionalOnClass({SendMessageHook.class, ConsumeMessageHook.class})
@ConditionalOnProperty(prefix = "youngerier.rocketmq", name = "enabled", matchIfMissing = true)
public class RocketMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TraceSendMessageHook traceSendMessageHook() {
        return new TraceSendMessageHook();
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceConsumeMessageHook traceConsumeMessageHook() {
        return new TraceConsumeMessageHook();
    }

    @Bean
    public static RocketMqTraceHookRegistrar rocketMqTraceHookRegistrar(
            ObjectProvider<TraceSendMessageHook> sendHookProvider,
            ObjectProvider<TraceConsumeMessageHook> consumeHookProvider) {
        return new RocketMqTraceHookRegistrar(sendHookProvider, consumeHookProvider);
    }

    /**
     * 便捷发送封装：rocketmq-spring 装配好 RocketMQTemplate 后生效。
     *
     * <p>必须同时判断 {@code @ConditionalOnBean}：rocketmq-spring 的
     * {@code RocketMQAutoConfiguration} 在缺少 {@code rocketmq.name-server} 配置时
     * 会跳过全部 bean（含 {@code RocketMQTemplate}）的创建，但 {@code RocketMQTemplate}
     * 这个<em>类</em>仍在 classpath 上。只判断 {@code @ConditionalOnClass} 会让本 Bean
     * 尝试注入一个不存在的 {@code RocketMQTemplate}，使使用方应用启动失败。
     */
    @Bean
    @ConditionalOnClass(RocketMQTemplate.class)
    @ConditionalOnBean(RocketMQTemplate.class)
    @ConditionalOnMissingBean
    public RocketMqProducer rocketMqProducer(RocketMQTemplate rocketMQTemplate) {
        return new RocketMqProducer(rocketMQTemplate);
    }

    /**
     * 仅 classpath 存在 rocketmq-spring 时，注册 RocketMQTemplate / 监听器容器的 Hook 识别逻辑
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {
            "org.apache.rocketmq.spring.core.RocketMQTemplate",
            "org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer"
    })
    static class RocketMqSpringRegistrarConfiguration {

        @Bean
        public static RocketMqSpringTraceHookRegistrar rocketMqSpringTraceHookRegistrar(
                RocketMqTraceHookRegistrar delegate) {
            return new RocketMqSpringTraceHookRegistrar(delegate);
        }
    }
}
