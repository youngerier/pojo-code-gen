package io.github.youngerier.support.trace.autoconfigure;

import io.github.youngerier.support.trace.MdcTaskDecorator;
import io.github.youngerier.support.trace.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.task.TaskDecorator;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link TraceAutoConfiguration}：
 *
 * <ul>
 *     <li>Servlet 环境下注册 traceId 过滤器。</li>
 *     <li>装配 {@link MdcTaskDecorator}，使异步任务继承 traceId。</li>
 *     <li>使用方自定义 {@link TaskDecorator} 时让位。</li>
 * </ul>
 */
class TraceAutoConfigurationTest {

    /** 模拟使用方已有的 TaskDecorator */
    @Configuration(proxyBeanMethods = false)
    static class CustomTaskDecoratorConfiguration {

        @Bean
        public TaskDecorator customTaskDecorator() {
            return runnable -> runnable;
        }
    }

    private AnnotationConfigWebApplicationContext refreshWeb(Class<?>... configurations) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(configurations);
        context.refresh();
        return context;
    }

    @Test
    void registersTraceIdFilterInServletContext() {
        try (AnnotationConfigWebApplicationContext context = refreshWeb(TraceAutoConfiguration.class)) {
            FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
            assertTrue(registration.getFilter() instanceof TraceIdFilter);
            assertEquals("traceIdFilter", registration.getFilterName());
        }
    }

    @Test
    void registersMdcTaskDecoratorForAsyncPropagation() {
        try (AnnotationConfigWebApplicationContext context = refreshWeb(TraceAutoConfiguration.class)) {
            assertTrue(context.getBean(TaskDecorator.class) instanceof MdcTaskDecorator);
        }
    }

    @Test
    void consumerTaskDecoratorTakesPrecedence() {
        try (AnnotationConfigWebApplicationContext context =
                     refreshWeb(CustomTaskDecoratorConfiguration.class, TraceAutoConfiguration.class)) {
            TaskDecorator decorator = context.getBean(TaskDecorator.class);
            assertEquals(1, context.getBeanNamesForType(TaskDecorator.class).length,
                    "使用方已有 TaskDecorator 时不得再注册");
            assertTrue(!(decorator instanceof MdcTaskDecorator));
        }
    }

    /**
     * 过滤器的开关：{@code youngerier.trace.enabled=false} 时不注册过滤器，
     * 但异步透传仍保持默认开启。
     */
    @Test
    void traceFilterCanBeDisabledByProperty() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("test", Map.of("youngerier.trace.enabled", "false")));
            context.register(TraceAutoConfiguration.class);
            context.refresh();

            assertEquals(0, context.getBeanNamesForType(FilterRegistrationBean.class).length);
            assertTrue(context.getBean(TaskDecorator.class) instanceof MdcTaskDecorator);
        }
    }
}
