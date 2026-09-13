package io.github.youngerier.support.autoconfigure;

import io.github.youngerier.support.audit.AuditAspect;
import io.github.youngerier.support.audit.AuditSink;
import io.github.youngerier.support.audit.Slf4jAuditSink;
import io.github.youngerier.support.trace.TraceIdFilter;
import io.github.youngerier.support.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在最小 Servlet Web 上下文中验证自动装配真正注册了默认 Bean，
 * 且使用方自定义 Bean 可以覆盖默认实现。
 */
class AutoConfigurationTest {

    @Test
    void webSupportRegistersExceptionHandlerAndTraceFilter() {
        try (AnnotationConfigWebApplicationContext context = refresh(WebSupportAutoConfiguration.class)) {
            assertNotNull(context.getBean(GlobalExceptionHandler.class));
            FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
            assertTrue(registration.getFilter() instanceof TraceIdFilter);
        }
    }

    @Test
    void auditRegistersAspectAndDefaultSink() {
        try (AnnotationConfigWebApplicationContext context = refresh(AuditAutoConfiguration.class)) {
            assertNotNull(context.getBean(AuditAspect.class));
            assertTrue(context.getBean(AuditSink.class) instanceof Slf4jAuditSink);
            assertNotNull(context.getBean("auditExecutor"));
        }
    }

    @Test
    void customAuditSinkOverridesDefault() {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(CustomSinkConfiguration.class, AuditAutoConfiguration.class);
            context.refresh();
            assertTrue(context.getBean(AuditSink.class) instanceof CustomAuditSink);
        }
    }

    @org.springframework.context.annotation.Configuration
    static class CustomSinkConfiguration {

        @org.springframework.context.annotation.Bean
        AuditSink customSink() {
            return new CustomAuditSink();
        }
    }

    static class CustomAuditSink implements AuditSink {
        @Override
        public void save(io.github.youngerier.support.audit.AuditEvent event) {
        }
    }

    private AnnotationConfigWebApplicationContext refresh(Class<?>... configurations) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(configurations);
        context.refresh();
        return context;
    }
}
