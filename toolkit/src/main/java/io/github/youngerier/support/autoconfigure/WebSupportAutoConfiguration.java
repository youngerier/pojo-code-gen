package io.github.youngerier.support.autoconfigure;

import io.github.youngerier.support.trace.TraceContext;
import io.github.youngerier.support.trace.TraceIdFilter;
import io.github.youngerier.support.web.GlobalExceptionHandler;
import io.github.youngerier.support.web.SecurityExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Web 基础能力自动装配：全局异常处理与 traceId 过滤器。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RestControllerAdvice.class)
public class WebSupportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * 仅 classpath 存在 Spring Security 时装配 401/403 处理
     */
    @Bean
    @ConditionalOnClass(AccessDeniedException.class)
    @ConditionalOnMissingBean
    public SecurityExceptionHandler securityExceptionHandler() {
        return new SecurityExceptionHandler();
    }

    @Bean
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
