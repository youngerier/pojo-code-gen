package com.abc.web.support.audit;

import com.abc.web.support.audit.annotations.Auditable;
import com.abc.web.support.audit.processor.AuditBeanPostProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.Advised;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 审计BeanPostProcessor测试
 * 
 * @author toolkit
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AuditBeanPostProcessorTest {
    
    @Mock
    private AuditService auditService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private AuditBeanPostProcessor processor;
    
    @BeforeEach
    void setUp() {
        processor = new AuditBeanPostProcessor(auditService, objectMapper);
    }
    
    @Test
    void testProcessBeanWithMethodLevelAnnotation() {
        // 给定：带有方法级别@Auditable注解的Bean
        TestServiceWithMethodAnnotation bean = new TestServiceWithMethodAnnotation();
        
        // 当：后处理器处理Bean
        Object result = processor.postProcessAfterInitialization(bean, "testBean");
        
        // 那么：应该返回代理对象
        assertNotNull(result);
        assertTrue(result instanceof Advised, "应该创建代理对象");
        assertNotSame(bean, result, "应该返回不同的对象实例");
    }
    
    @Test
    void testProcessBeanWithClassLevelAnnotation() {
        // 给定：带有类级别@Auditable注解的Bean
        TestServiceWithClassAnnotation bean = new TestServiceWithClassAnnotation();
        
        // 当：后处理器处理Bean
        Object result = processor.postProcessAfterInitialization(bean, "testBean");
        
        // 那么：应该返回代理对象
        assertNotNull(result);
        assertTrue(result instanceof Advised, "应该创建代理对象");
    }
    
    @Test
    void testProcessBeanWithoutAnnotation() {
        // 给定：没有@Auditable注解的Bean
        TestServiceWithoutAnnotation bean = new TestServiceWithoutAnnotation();
        
        // 当：后处理器处理Bean
        Object result = processor.postProcessAfterInitialization(bean, "testBean");
        
        // 那么：应该返回原始对象
        assertNotNull(result);
        assertSame(bean, result, "应该返回原始对象");
    }
    
    @Test
    void testSkipSpringInternalClass() {
        // 给定：Spring内部类
        Object springBean = new org.springframework.context.support.ApplicationContextAwareProcessor();
        
        // 当：后处理器处理Bean
        Object result = processor.postProcessAfterInitialization(springBean, "springBean");
        
        // 那么：应该跳过处理
        assertSame(springBean, result, "应该跳过Spring内部类");
    }
    
    @Test
    void testAuditMethodExecution() throws Throwable {
        // 给定：带有审计注解的服务
        TestServiceWithMethodAnnotation bean = new TestServiceWithMethodAnnotation();
        Object proxy = processor.postProcessAfterInitialization(bean, "testBean");
        
        // 当：调用审计方法
        ((TestServiceWithMethodAnnotation) proxy).auditedMethod("test");
        
        // 那么：应该调用审计服务（注意：这里需要更复杂的集成测试来验证）
        // 实际测试中需要Spring容器来完整验证审计功能
    }
    
    // 测试用的服务类
    
    /**
     * 带有方法级别注解的测试服务
     */
    static class TestServiceWithMethodAnnotation {
        
        @Auditable(operation = "测试操作", eventType = AuditEventType.BUSINESS_OPERATION)
        public void auditedMethod(String param) {
            // 测试方法
        }
        
        public void normalMethod() {
            // 普通方法，不会被审计
        }
    }
    
    /**
     * 带有类级别注解的测试服务
     */
    @Auditable(operation = "类级别审计", eventType = AuditEventType.BUSINESS_OPERATION)
    static class TestServiceWithClassAnnotation {
        
        public void method1() {
            // 会被审计
        }
        
        public void method2() {
            // 会被审计
        }
    }
    
    /**
     * 没有注解的测试服务
     */
    static class TestServiceWithoutAnnotation {
        
        public void normalMethod() {
            // 不会被审计
        }
    }
}