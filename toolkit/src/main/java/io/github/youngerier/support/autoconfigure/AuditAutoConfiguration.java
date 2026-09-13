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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;

/**
 * 审计能力自动装配。使用方式：类或方法上加 {@link Auditable} 即可，
 * 需要落库/上报时实现 {@link AuditSink} 注册为 Bean 覆盖默认日志输出。
 *
 * <p>配置项（前缀 {@code youngerier.audit}）：
 * <ul>
 *   <li>{@code enabled}：是否启用，默认 true</li>
 *   <li>{@code executor.core-size / max-size / queue-capacity}：审计线程池参数</li>
 *   <li>{@code executor.reject-policy}：满员策略 caller-runs（默认）/ abort / discard</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({Auditable.class, ProceedingJoinPoint.class, ObjectMapper.class})
@ConditionalOnProperty(prefix = "youngerier.audit", name = "enabled", matchIfMissing = true)
public class AuditAutoConfiguration {

    public static final String AUDIT_EXECUTOR = "auditExecutor";

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

    @Bean(name = AUDIT_EXECUTOR)
    @ConditionalOnMissingBean(name = AUDIT_EXECUTOR)
    public ThreadPoolTaskExecutor auditExecutor(
            @Value("${youngerier.audit.executor.core-size:2}") int coreSize,
            @Value("${youngerier.audit.executor.max-size:4}") int maxSize,
            @Value("${youngerier.audit.executor.queue-capacity:1000}") int queueCapacity,
            @Value("${youngerier.audit.executor.reject-policy:caller-runs}") String rejectPolicy) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("audit-");
        executor.setKeepAliveSeconds(60);
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(resolveRejectPolicy(rejectPolicy));
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditAspect auditAspect(AuditSink auditSink,
                                   AuditUserProvider auditUserProvider,
                                   ObjectProvider<ObjectMapper> objectMapperProvider,
                                   @Qualifier(AUDIT_EXECUTOR) Executor auditExecutor) {
        return new AuditAspect(auditSink, auditUserProvider,
                objectMapperProvider.getIfAvailable(), auditExecutor);
    }

    private RejectedExecutionHandler resolveRejectPolicy(String policy) {
        return switch (policy.trim().toLowerCase()) {
            case "abort" -> new AbortPolicy();
            case "discard" -> new DiscardPolicy();
            // 队列满时由调用线程执行，避免审计事件丢失
            default -> new CallerRunsPolicy();
        };
    }
}
