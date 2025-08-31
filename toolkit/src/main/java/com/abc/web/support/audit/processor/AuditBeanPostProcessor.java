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
 * 审计Bean后处理器
 * 统一的审计实现方案，基于BeanPostProcessor和MethodInterceptor
 * 自动为带有@Auditable注解的Bean创建代理并实现完整的审计功能
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
    
    // 缓存已处理的Bean，避免重复处理
    private final Map<String, Boolean> processedBeans = new ConcurrentHashMap<>();
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 避免重复处理同一个Bean
        if (processedBeans.containsKey(beanName)) {
            return bean;
        }
        
        Class<?> beanClass = bean.getClass();
        
        // 跳过Spring内部类和代理类
        if (isSpringInternalClass(beanClass)) {
            return bean;
        }
        
        // 检查是否需要审计
        if (needsAudit(beanClass)) {
            processedBeans.put(beanName, true);
            return createAuditProxy(bean);
        }
        
        return bean;
    }
    
    /**
     * 检查是否是Spring内部类
     */
    private boolean isSpringInternalClass(Class<?> clazz) {
        String className = clazz.getName();
        return className.startsWith("org.springframework.") ||
               className.startsWith("org.apache.catalina.") ||
               className.startsWith("com.sun.") ||
               className.contains("$$EnhancerBySpringCGLIB$$") ||
               className.contains("$$FastClassBySpringCGLIB$$");
    }
    
    /**
     * 检查Bean是否需要审计
     */
    private boolean needsAudit(Class<?> beanClass) {
        // 检查类级别的@Auditable注解
        if (AnnotationUtils.findAnnotation(beanClass, Auditable.class) != null) {
            return true;
        }
        
        // 检查方法级别的@Auditable注解
        for (Method method : beanClass.getDeclaredMethods()) {
            if (AnnotationUtils.findAnnotation(method, Auditable.class) != null) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 创建审计代理
     */
    private Object createAuditProxy(Object bean) {
        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.addAdvice(new AuditMethodInterceptor());
        proxyFactory.setProxyTargetClass(true); // 使用CGLIB代理
        return proxyFactory.getProxy();
    }
    
    /**
     * 审计方法拦截器
     * 整合了原AuditAspect的完整功能
     */
    private class AuditMethodInterceptor implements MethodInterceptor {
        
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Method method = invocation.getMethod();
            
            // 获取@Auditable注解
            Auditable auditable = getAuditableAnnotation(method, invocation.getThis().getClass());
            
            if (auditable == null) {
                return invocation.proceed();
            }
            
            // 检查审计条件
            if (!shouldAudit(invocation, auditable)) {
                return invocation.proceed();
            }
            
            AuditEvent auditEvent = createAuditEvent(invocation, auditable);
            long startTime = System.currentTimeMillis();
            
            try {
                Object result = invocation.proceed();
                
                // 记录成功结果
                auditEvent.success();
                auditEvent.setDuration(System.currentTimeMillis() - startTime);
                
                // 记录返回值
                if (auditable.includeResult() && result != null) {
                    auditEvent.setAfterData(serializeObject(result));
                }
                
                saveAuditEvent(auditEvent, auditable.async());
                return result;
                
            } catch (Throwable throwable) {
                // 记录异常信息
                auditEvent.failure(throwable.getMessage());
                auditEvent.setDuration(System.currentTimeMillis() - startTime);
                
                if (auditable.includeException()) {
                    auditEvent.setErrorMessage(getFullExceptionMessage(throwable));
                }
                
                saveAuditEvent(auditEvent, auditable.async());
                throw throwable;
            }
        }
        
        /**
         * 获取@Auditable注解（方法级别优先于类级别）
         */
        private Auditable getAuditableAnnotation(Method method, Class<?> targetClass) {
            // 优先检查方法级别注解
            Auditable methodLevel = AnnotationUtils.findAnnotation(method, Auditable.class);
            if (methodLevel != null) {
                return methodLevel;
            }
            
            // 检查类级别注解
            return AnnotationUtils.findAnnotation(targetClass, Auditable.class);
        }
        
        /**
         * 检查是否应该进行审计
         */
        private boolean shouldAudit(MethodInvocation invocation, Auditable auditable) {
            String condition = auditable.condition();
            if (!StringUtils.hasText(condition)) {
                return true;
            }
            
            try {
                StandardEvaluationContext context = createEvaluationContext(invocation);
                return Boolean.TRUE.equals(parser.parseExpression(condition).getValue(context, Boolean.class));
            } catch (Exception e) {
                log.warn("审计条件表达式解析失败: {}", condition, e);
                return true; // 默认进行审计
            }
        }
        
        /**
         * 创建审计事件
         */
        private AuditEvent createAuditEvent(MethodInvocation invocation, Auditable auditable) {
            AuditEvent event = AuditEvent.create();
            
            // 设置基本信息
            event.setEventType(auditable.eventType());
            event.setOperation(getOperation(invocation, auditable));
            event.setDescription(getDescription(invocation, auditable));
            event.setResourceType(auditable.resourceType());
            event.setModuleName(auditable.module());
            
            // 设置用户信息
            setUserInfo(event);
            
            // 设置请求信息
            setRequestInfo(event);
            
            // 设置业务标识
            setBusinessKey(event, invocation, auditable);
            
            // 设置参数信息
            if (auditable.includeParameters()) {
                setParameterInfo(event, invocation, auditable);
            }
            
            return event;
        }
        
        /**
         * 获取操作名称
         */
        private String getOperation(MethodInvocation invocation, Auditable auditable) {
            if (StringUtils.hasText(auditable.operation())) {
                return auditable.operation();
            }
            
            // 默认使用方法名
            return invocation.getMethod().getName();
        }
        
        /**
         * 获取操作描述
         */
        private String getDescription(MethodInvocation invocation, Auditable auditable) {
            if (StringUtils.hasText(auditable.description())) {
                return auditable.description();
            }
            
            // 默认使用类名+方法名
            String className = invocation.getThis().getClass().getSimpleName();
            String methodName = invocation.getMethod().getName();
            return className + "." + methodName;
        }
        
        /**
         * 设置用户信息
         */
        private void setUserInfo(AuditEvent event) {
            try {
                // TODO: 从Security Context获取用户信息
                // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                // if (authentication != null && authentication.isAuthenticated()) {
                //     UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                //     event.setUserId(userDetails.getUsername());
                //     event.setUsername(userDetails.getUsername());
                // }
                
                // 临时设置默认值
                event.setUserId("system");
                event.setUsername("system");
            } catch (Exception e) {
                log.warn("获取用户信息失败", e);
            }
        }
        
        /**
         * 设置请求信息
         */
        private void setRequestInfo(AuditEvent event) {
            try {
                ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    event.setClientIp(getClientIp(request));
                    event.setUserAgent(request.getHeader("User-Agent"));
                    event.setRequestPath(request.getRequestURI());
                    event.setRequestMethod(request.getMethod());
                    event.setSessionId(request.getSession().getId());
                    
                    // 设置请求ID（如果有的话）
                    String requestId = request.getHeader("X-Request-ID");
                    if (requestId != null) {
                        event.setRequestId(requestId);
                    }
                }
            } catch (Exception e) {
                log.warn("获取请求信息失败", e);
            }
        }
        
        /**
         * 设置业务标识
         */
        private void setBusinessKey(AuditEvent event, MethodInvocation invocation, Auditable auditable) {
            String businessKeyExpression = auditable.businessKey();
            if (!StringUtils.hasText(businessKeyExpression)) {
                return;
            }
            
            try {
                StandardEvaluationContext context = createEvaluationContext(invocation);
                Object businessKey = parser.parseExpression(businessKeyExpression).getValue(context);
                if (businessKey != null) {
                    event.setBusinessKey(businessKey.toString());
                }
            } catch (Exception e) {
                log.warn("解析业务标识失败: {}", businessKeyExpression, e);
            }
        }
        
        /**
         * 设置参数信息
         */
        private void setParameterInfo(AuditEvent event, MethodInvocation invocation, Auditable auditable) {
            try {
                Object[] args = invocation.getArguments();
                if (args == null || args.length == 0) {
                    return;
                }
                
                Map<String, Object> params = new HashMap<>();
                String[] paramNames = getParameterNames(invocation.getMethod());
                
                // 获取方法参数注解
                Annotation[][] paramAnnotations = invocation.getMethod().getParameterAnnotations();
                
                for (int i = 0; i < args.length; i++) {
                    String paramName = i < paramNames.length ? paramNames[i] : "param" + i;
                    
                    // 检查是否应该忽略参数
                    if (shouldIgnoreParam(i, paramName, paramAnnotations[i], auditable)) {
                        continue;
                    }
                    
                    Object paramValue = args[i];
                    
                    // 敏感参数脱敏
                    paramValue = maskSensitiveParam(i, paramName, paramValue, paramAnnotations[i], auditable, invocation);
                    
                    params.put(paramName, paramValue);
                }
                
                if (!params.isEmpty()) {
                    event.setBeforeData(serializeObject(params));
                }
            } catch (Exception e) {
                log.warn("设置参数信息失败", e);
            }
        }
        
        /**
         * 创建SpEL表达式上下文
         */
        private StandardEvaluationContext createEvaluationContext(MethodInvocation invocation) {
            StandardEvaluationContext context = new StandardEvaluationContext();
            
            Object[] args = invocation.getArguments();
            String[] paramNames = getParameterNames(invocation.getMethod());
            
            for (int i = 0; i < args.length; i++) {
                String paramName = i < paramNames.length ? paramNames[i] : "param" + i;
                context.setVariable(paramName, args[i]);
            }
            
            return context;
        }
        
        /**
         * 获取参数名称
         */
        private String[] getParameterNames(Method method) {
            // 简单实现，使用参数索引
            String[] names = new String[method.getParameterCount()];
            for (int i = 0; i < names.length; i++) {
                names[i] = "param" + i;
            }
            return names;
        }
        
        /**
         * 检查是否应该忽略参数
         */
        private boolean shouldIgnoreParam(int paramIndex, String paramName, Annotation[] paramAnnotations, Auditable auditable) {
            // 优先检查参数注解
            if (auditable.enableParamAnnotations()) {
                for (Annotation annotation : paramAnnotations) {
                    if (annotation instanceof IgnoreParam) {
                        return true;
                    }
                }
            }
            
            // 检查索引方式配置（向后兼容）
            if (Arrays.stream(auditable.ignoreParams()).anyMatch(index -> index == paramIndex)) {
                return true;
            }
            
            // 检查参数名称方式配置
            if (Arrays.stream(auditable.ignoreParamNames()).anyMatch(name -> name.equals(paramName))) {
                return true;
            }
            
            return false;
        }
        
        /**
         * 敏感参数脱敏
         */
        private Object maskSensitiveParam(int paramIndex, String paramName, Object paramValue, 
                                        Annotation[] paramAnnotations, Auditable auditable, MethodInvocation invocation) {
            
            // 优先检查参数注解
            if (auditable.enableParamAnnotations()) {
                for (Annotation annotation : paramAnnotations) {
                    if (annotation instanceof SensitiveParam) {
                        SensitiveParam sensitiveParam = (SensitiveParam) annotation;
                        return DataMaskingUtils.maskData(paramValue, sensitiveParam.strategy(), sensitiveParam.customExpression());
                    }
                }
            }
            
            // 检查索引方式配置（向后兼容）
            if (Arrays.stream(auditable.sensitiveParams()).anyMatch(index -> index == paramIndex)) {
                return maskSensitiveData(paramValue);
            }
            
            // 检查参数名称方式配置
            if (Arrays.stream(auditable.sensitiveParamNames()).anyMatch(name -> name.equals(paramName))) {
                return maskSensitiveData(paramValue);
            }
            
            // 检查SpEL表达式配置
            if (StringUtils.hasText(auditable.sensitiveParamExpression())) {
                try {
                    StandardEvaluationContext context = createEvaluationContext(invocation);
                    Boolean isSensitive = parser.parseExpression(auditable.sensitiveParamExpression())
                        .getValue(context, Boolean.class);
                    if (Boolean.TRUE.equals(isSensitive)) {
                        return maskSensitiveData(paramValue);
                    }
                } catch (Exception e) {
                    log.warn("敏感参数表达式解析失败: {}", auditable.sensitiveParamExpression(), e);
                }
            }
            
            return paramValue;
        }
        
        /**
         * 脱敏敏感数据（默认实现）
         */
        private Object maskSensitiveData(Object data) {
            if (data == null) {
                return null;
            }
            
            String str = data.toString();
            if (str.length() <= 4) {
                return "****";
            }
            
            return str.substring(0, 2) + "****" + str.substring(str.length() - 2);
        }
        
        /**
         * 获取客户端IP
         */
        private String getClientIp(HttpServletRequest request) {
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
        private String serializeObject(Object obj) {
            try {
                return objectMapper.writeValueAsString(obj);
            } catch (Exception e) {
                return obj != null ? obj.toString() : null;
            }
        }
        
        /**
         * 获取完整异常消息
         */
        private String getFullExceptionMessage(Throwable throwable) {
            StringBuilder sb = new StringBuilder();
            sb.append(throwable.getClass().getSimpleName()).append(": ").append(throwable.getMessage());
            
            Throwable cause = throwable.getCause();
            while (cause != null) {
                sb.append(" -> ").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
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