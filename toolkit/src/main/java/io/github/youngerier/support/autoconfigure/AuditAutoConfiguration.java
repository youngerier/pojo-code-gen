package io.github.youngerier.support.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.youngerier.support.audit.AuditAspect;
import io.github.youngerier.support.audit.AuditSink;
import io.github.youngerier.support.audit.AuditUserProvider;
import io.github.youngerier.support.audit.Slf4jAuditSink;
import io.github.youngerier.support.audit.annotations.Auditable;
import io.github.youngerier.support.trace.MdcTaskDecorator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 审计能力自动装配。使用方式：类或方法上加 {@link Auditable} 即可，
 * 需要落库/上报时实现 {@link AuditSink} 注册为 Bean 覆盖默认日志输出。
 *
 * <p>通过 {@code youngerier.audit.enabled=false} 可关闭。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({Auditable.class, ProceedingJoinPoint.class})
@ConditionalOnProperty(prefix = "youngerier.audit", name = "enabled", matchIfMissing = true)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditSink auditSink() {
        return new Slf4jAuditSink();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditUserProvider auditUserProvider() {
        return () -> null;
    }

    @Bean(name = "auditExecutor")
    @ConditionalOnMissingBean(name = "auditExecutor")
    public ThreadPoolTaskExecutor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("audit-");
        executor.setKeepAliveSeconds(60);
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditAspect auditAspect(AuditSink auditSink,
                                   AuditUserProvider auditUserProvider,
                                   ObjectProvider<ObjectMapper> objectMapperProvider,
                                   Executor auditExecutor) {
        return new AuditAspect(auditSink, auditUserProvider,
                objectMapperProvider.getIfAvailable(), auditExecutor);
    }
}
