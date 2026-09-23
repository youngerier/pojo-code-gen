package io.github.youngerier.support.trace.autoconfigure;

import io.github.youngerier.support.trace.MdcTaskDecorator;
import io.github.youngerier.support.trace.TraceContext;
import io.github.youngerier.support.trace.TraceIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskDecorator;

/**
 * 链路透传自动装配：
 *
 * <ul>
 *     <li>{@link TraceIdFilter}：Servlet Web 环境下为每个请求准备 traceId，
 *     需要 spring-web 与 spring-boot（{@code FilterRegistrationBean}）。</li>
 *     <li>{@link MdcTaskDecorator}：注册为容器内唯一的 {@link TaskDecorator}，
 *     让 Spring Boot 自动配置的 {@code applicationTaskExecutor} 与 {@code @Async}
 *     继承提交线程的 MDC。此前 {@code MdcTaskDecorator} 从未被装配，是死代码。</li>
 * </ul>
 *
 * <p>两类能力拆成独立嵌套配置类，并用 {@code name} 字符串形式声明可选类条件：
 * 方法级 {@code @ConditionalOnClass} 在注解元数据以反射方式读取时会被静默跳过，
 * 进而把缺失的可选依赖变成 {@code NoClassDefFoundError}。
 */
@AutoConfiguration
public class TraceAutoConfiguration {

    /**
     * 异步任务透传：只依赖 spring-core，不要求 Web 环境。
     */
    @Configuration(proxyBeanMethods = false)
    static class AsyncPropagationConfiguration {

        @Bean
        @ConditionalOnMissingBean(TaskDecorator.class)
        @ConditionalOnProperty(prefix = "youngerier.trace", name = "propagate-to-async", matchIfMissing = true)
        public TaskDecorator mdcTaskDecorator() {
            return new MdcTaskDecorator();
        }
    }

    /**
     * Servlet 入站 traceId：需要 spring-web 与 spring-boot。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = {
            "org.springframework.web.filter.OncePerRequestFilter",
            "org.springframework.boot.web.servlet.FilterRegistrationBean"
    })
    static class ServletTraceConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "traceIdFilter")
        @ConditionalOnProperty(prefix = "youngerier.trace", name = "enabled", matchIfMissing = true)
        public FilterRegistrationBean<TraceIdFilter> traceIdFilter(
                @Value("${youngerier.trace.header-name:" + TraceContext.TRACE_ID_HEADER + "}") String headerName) {
            FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new TraceIdFilter(headerName));
            registration.addUrlPatterns("/*");
            registration.setName("traceIdFilter");
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
            return registration;
        }
    }
}
