package com.abc.web.support.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 审计配置类
 * 基于BeanPostProcessor的统一审计实现方案
 * 
 * 注意：
 * - 使用Spring Boot默认的ObjectMapper进行数据序列化
 * - 如果需要自定义序列化配置，请在application.yml中配置
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class AuditConfig {
    
    /**
     * 审计专用线程池
     * 用于异步处理审计事件，避免对主业务线程造成影响
     */
    @Bean("auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);  // 核心线程数
        executor.setMaxPoolSize(4);   // 最大线程数
        executor.setQueueCapacity(1000); // 队列容量
        executor.setThreadNamePrefix("audit-"); // 线程名前缀
        executor.setKeepAliveSeconds(60); // 空闲线程保持时间
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}