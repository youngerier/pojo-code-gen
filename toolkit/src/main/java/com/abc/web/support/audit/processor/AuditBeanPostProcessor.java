package com.abc.web.support.audit.processor;

import com.abc.web.support.audit.*;
import com.abc.web.support.audit.annotations.Auditable;
import com.abc.web.support.audit.annotations.IgnoreParam;
import com.abc.web.support.audit.annotations.SensitiveParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 审计Bean后处理器 - 简化版本
 * 基于BeanPostProcessor自动为带有@Auditable注解的Bean创建审计代理
 * 
 * 主要简化内容：
 * 1. 减少方法数量，合并相关逻辑
 * 2. 简化参数处理逻辑
 * 3. 统一异常处理
 * 4. 提高代码可读性
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditBeanPostProcessor implements BeanPostProcessor {
    
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Boolean> processedBeans = new ConcurrentHashMap<>();
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (shouldSkipBean(beanName, bean.getClass()) || !hasAuditableAnnotation(bean.getClass())) {
            return bean;
        }
        
        processedBeans.put(beanName, true);
        return createAuditProxy(bean);
    }
    
    private boolean shouldSkipBean(String beanName, Class<?> beanClass) {
        return processedBeans.containsKey(beanName) || isSpringInternalClass(beanClass);
    }
    
    private boolean isSpringInternalClass(Class<?> clazz) {
        String name = clazz.getName();
        return name.startsWith("org.springframework.") || 
               name.startsWith("org.apache.catalina.") ||
               name.contains("$$EnhancerBySpringCGLIB$$");
    }
    
    private boolean hasAuditableAnnotation(Class<?> beanClass) {
        // 检查类级别注解
        if (AnnotationUtils.findAnnotation(beanClass, Auditable.class) != null) {
            return true;
        }
        
        // 检查方法级别注解
        return Arrays.stream(beanClass.getDeclaredMethods())
                .anyMatch(method -> AnnotationUtils.findAnnotation(method, Auditable.class) != null);
    }
    
    private Object createAuditProxy(Object bean) {
        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.addAdvice(new AuditMethodInterceptor());
        proxyFactory.setProxyTargetClass(true);
        return proxyFactory.getProxy();
    }
    
    /**
     * 审计方法拦截器 - 简化版本
     */
    private class AuditMethodInterceptor implements MethodInterceptor {
        
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Auditable auditable = findAuditableAnnotation(invocation);
            if (auditable == null || !shouldAudit(invocation, auditable)) {
                return invocation.proceed();
            }
            
            return executeWithAudit(invocation, auditable);
        }
        
        /**
         * 查找@Auditable注解（方法优先于类）
         */
        private Auditable findAuditableAnnotation(MethodInvocation invocation) {
            Method method = invocation.getMethod();
            Class<?> targetClass = invocation.getThis().getClass();
            
            Auditable methodLevel = AnnotationUtils.findAnnotation(method, Auditable.class);
            return methodLevel != null ? methodLevel : AnnotationUtils.findAnnotation(targetClass, Auditable.class);
        }
        
        /**
         * 检查是否应该执行审计
         */
        private boolean shouldAudit(MethodInvocation invocation, Auditable auditable) {
            String condition = auditable.condition();
            if (!StringUtils.hasText(condition)) {
                return true;
            }
            
            try {
                StandardEvaluationContext context = buildSpelContext(invocation);
                return Boolean.TRUE.equals(parser.parseExpression(condition).getValue(context, Boolean.class));
            } catch (Exception e) {
                log.warn("审计条件解析失败: {}", condition, e);
                return true; // 默认执行审计
            }
        }
        
        /**
         * 执行带审计的方法调用
         */
        private Object executeWithAudit(MethodInvocation invocation, Auditable auditable) throws Throwable {
            AuditEvent auditEvent = createAuditEvent(invocation, auditable);
            long startTime = System.currentTimeMillis();
            
            try {
                Object result = invocation.proceed();
                
                // 记录成功
                auditEvent.success();
                auditEvent.setDuration(System.currentTimeMillis() - startTime);
                
                if (auditable.includeResult() && result != null) {
                    auditEvent.setAfterData(serialize(result));
                }
                
                saveAuditEvent(auditEvent, auditable.async());
                return result;
                
            } catch (Throwable throwable) {
                // 记录失败
                auditEvent.failure(throwable.getMessage());
                auditEvent.setDuration(System.currentTimeMillis() - startTime);
                
                if (auditable.includeException()) {
                    auditEvent.setErrorMessage(buildExceptionMessage(throwable));
                }
                
                saveAuditEvent(auditEvent, auditable.async());
                throw throwable;
            }
        }
        
        /**
         * 创建审计事件
         */
        private AuditEvent createAuditEvent(MethodInvocation invocation, Auditable auditable) {
            AuditEvent event = AuditEvent.create();
            
            // 基本信息
            event.setEventType(auditable.eventType());
            event.setOperation(getOperationName(invocation, auditable));
            event.setDescription(getDescription(invocation, auditable));
            event.setResourceType(auditable.resourceType());
            event.setModuleName(auditable.module());
            
            // 上下文信息
            setContextInfo(event);
            
            // 业务信息
            setBusinessKey(event, invocation, auditable);
            if (auditable.includeParameters()) {
                setParameterInfo(event, invocation, auditable);
            }
            
            return event;
        }
        
        private String getOperationName(MethodInvocation invocation, Auditable auditable) {
            return StringUtils.hasText(auditable.operation()) 
                ? auditable.operation() 
                : invocation.getMethod().getName();
        }
        
        private String getDescription(MethodInvocation invocation, Auditable auditable) {
            if (StringUtils.hasText(auditable.description())) {
                return auditable.description();
            }
            
            String className = invocation.getThis().getClass().getSimpleName();
            String methodName = invocation.getMethod().getName();
            return className + "." + methodName;
        }
        
        /**
         * 设置上下文信息（用户、请求等）
         */
        private void setContextInfo(AuditEvent event) {
            // 设置用户信息
            event.setUserId("system");
            event.setUsername("system");
            
            // 设置请求信息
            try {
                HttpServletRequest request = getCurrentRequest();
                if (request != null) {
                    event.setClientIp(extractClientIp(request));
                    event.setUserAgent(request.getHeader("User-Agent"));
                    event.setRequestPath(request.getRequestURI());
                    event.setRequestMethod(request.getMethod());
                    event.setSessionId(request.getSession().getId());
                    
                    String requestId = request.getHeader("X-Request-ID");
                    if (requestId != null) {
                        event.setRequestId(requestId);
                    }
                }
            } catch (Exception e) {
                log.warn("获取请求信息失败", e);
            }
        }
        
        private HttpServletRequest getCurrentRequest() {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        }
        
        private void setBusinessKey(AuditEvent event, MethodInvocation invocation, Auditable auditable) {
            String businessKeyExpression = auditable.businessKey();
            if (!StringUtils.hasText(businessKeyExpression)) {
                return;
            }
            
            try {
                StandardEvaluationContext context = buildSpelContext(invocation);
                Object businessKey = parser.parseExpression(businessKeyExpression).getValue(context);
                if (businessKey != null) {
                    event.setBusinessKey(businessKey.toString());
                }
            } catch (Exception e) {
                log.warn("解析业务标识失败: {}", businessKeyExpression, e);
            }
        }
        
        /**
         * 设置参数信息（简化版本）
         */
        private void setParameterInfo(AuditEvent event, MethodInvocation invocation, Auditable auditable) {
            try {
                Object[] args = invocation.getArguments();
                if (args.length == 0) {
                    return;
                }
                
                Map<String, Object> params = new HashMap<>();
                Annotation[][] paramAnnotations = invocation.getMethod().getParameterAnnotations();
                
                for (int i = 0; i < args.length; i++) {
                    String paramName = "param" + i;
                    
                    // 检查是否忽略参数
                    if (shouldIgnoreParameter(paramName, paramAnnotations[i], auditable)) {
                        continue;
                    }
                    
                    // 处理敏感参数
                    Object paramValue = processSensitiveParameter(args[i], paramAnnotations[i], auditable);
                    params.put(paramName, paramValue);
                }
                
                if (!params.isEmpty()) {
                    event.setBeforeData(serialize(params));
                }
            } catch (Exception e) {
                log.warn("设置参数信息失败", e);
            }
        }
        
        private boolean shouldIgnoreParameter(String paramName, Annotation[] annotations, Auditable auditable) {
            // 检查参数注解
            if (auditable.enableParamAnnotations()) {
                for (Annotation annotation : annotations) {
                    if (annotation instanceof IgnoreParam) {
                        return true;
                    }
                }
            }
            
            // 检查配置的忽略参数名
            return Arrays.stream(auditable.ignoreParamNames()).anyMatch(name -> name.equals(paramName));
        }
        
        private Object processSensitiveParameter(Object value, Annotation[] annotations, Auditable auditable) {
            if (!auditable.enableParamAnnotations()) {
                return value;
            }
            
            // 检查敏感参数注解
            for (Annotation annotation : annotations) {
                if (annotation instanceof SensitiveParam sensitiveParam) {
                    return NestedMaskingProcessor.maskNestedData(
                        value, 
                        sensitiveParam.strategy(), 
                        sensitiveParam.customExpression(),
                        sensitiveParam.fieldPaths(), 
                        sensitiveParam.autoNested()
                    );
                }
            }
            
            return value;
        }
        
        /**
         * 构建SpEL表达式上下文
         */
        private StandardEvaluationContext buildSpelContext(MethodInvocation invocation) {
            StandardEvaluationContext context = new StandardEvaluationContext();
            Object[] args = invocation.getArguments();
            
            for (int i = 0; i < args.length; i++) {
                context.setVariable("param" + i, args[i]);
            }
            
            return context;
        }
        
        /**
         * 提取客户端IP
         */
        private String extractClientIp(HttpServletRequest request) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        }
        
        /**
         * 序列化对象
         */
        private String serialize(Object obj) {
            try {
                return objectMapper.writeValueAsString(obj);
            } catch (Exception e) {
                return obj != null ? obj.toString() : null;
            }
        }
        
        /**
         * 构建异常消息
         */
        private String buildExceptionMessage(Throwable throwable) {
            StringBuilder sb = new StringBuilder();
            sb.append(throwable.getClass().getSimpleName()).append(": ").append(throwable.getMessage());
            
            Throwable cause = throwable.getCause();
            while (cause != null) {
                sb.append(" -> ").append(cause.getClass().getSimpleName())
                  .append(": ").append(cause.getMessage());
                cause = cause.getCause();
            }
            
            return sb.toString();
        }
        
        /**
         * 保存审计事件
         */
        private void saveAuditEvent(AuditEvent event, boolean async) {
            if (async) {
                auditService.saveAuditEventAsync(event);
            } else {
                auditService.saveAuditEvent(event);
            }
        }
    }
}