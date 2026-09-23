package io.github.youngerier.support.autoconfigure;

import io.github.youngerier.support.i18n.ExceptionMessageProvider;
import io.github.youngerier.support.i18n.ResourceExceptionMessageProvider;
import io.github.youngerier.support.trace.TraceContext;
import io.github.youngerier.support.trace.TraceIdFilter;
import io.github.youngerier.support.web.GlobalExceptionHandler;
import io.github.youngerier.support.web.SecurityExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.List;

/**
 * Web 基础能力自动装配：全局异常处理与 traceId 过滤器。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RestControllerAdvice.class)
public class WebSupportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(ObjectProvider<MessageSource> messageSourceProvider,
                                                          ObjectProvider<ExceptionMessageProvider> messageProviders) {
        List<ExceptionMessageProvider> providers = messageProviders.orderedStream().toList();
        return new GlobalExceptionHandler(messageSourceProvider.getIfAvailable(), providers);
    }

    /**
     * 仅 classpath 存在 Spring Security 时装配 401/403 处理。
     *
     * <p><b>必须放在独立嵌套配置类上做「类级别」条件判断</b>：方法级 {@code @ConditionalOnClass}
     * 只在注解元数据由 ASM 读取时（真实自动装配路径）可靠；当元数据以反射方式读取时
     * （{@code @Import(WebSupportAutoConfiguration.class)}、组件扫描、直接
     * {@code register(WebSupportAutoConfiguration.class)}）该条件会被静默跳过，
     * {@link SecurityExceptionHandler} 仍会被实例化，随后 Spring 内省该类时因其
     * {@code @ExceptionHandler(AuthenticationException.class)} 引用缺失的 spring-security 类
     * 而抛 {@code NoClassDefFoundError}，直接导致应用启动失败。
     *
     * <p>类级别条件 + {@code name} 字符串形式在 ASM 与反射两条路径下都安全。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.security.access.AccessDeniedException")
    static class SpringSecurityExceptionConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public SecurityExceptionHandler securityExceptionHandler() {
            return new SecurityExceptionHandler();
        }
    }

    /**
     * 配置文件异常消息来源：配置 youngerier.exception-message.basenames 后生效，
     * basenames 为逗号分隔的多个 ResourceBundle 路径（不含 .properties 后缀），
     * 如 youngerier.exception-message.basenames=exception-messages,config/other-messages。
     */
    @Bean
    @ConditionalOnProperty(prefix = "youngerier.exception-message", name = "basenames")
    @ConditionalOnMissingBean(ResourceExceptionMessageProvider.class)
    public ResourceExceptionMessageProvider resourceExceptionMessageProvider(
            @Value("${youngerier.exception-message.basenames}") String basenames) {
        String[] names = Arrays.stream(basenames.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toArray(String[]::new);
        return new ResourceExceptionMessageProvider(names);
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
