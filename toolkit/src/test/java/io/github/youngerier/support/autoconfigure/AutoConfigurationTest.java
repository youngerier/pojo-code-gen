package io.github.youngerier.support.autoconfigure;

import io.github.youngerier.support.trace.TraceIdFilter;
import io.github.youngerier.support.web.GlobalExceptionHandler;
import io.github.youngerier.support.web.SecurityExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在最小 Servlet Web 上下文中验证自动装配真正注册了默认 Bean。
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
    void securityHandlerRegisteredWhenSpringSecurityPresent() {
        try (AnnotationConfigWebApplicationContext context = refresh(WebSupportAutoConfiguration.class)) {
            assertNotNull(context.getBean(SecurityExceptionHandler.class));
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
