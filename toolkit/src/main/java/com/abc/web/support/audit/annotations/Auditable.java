package com.abc.web.support.audit.annotations;

import com.abc.web.support.audit.AuditEventType;

import java.lang.annotation.*;

/**
 * 审计注解
 * 用于标记需要进行审计记录的方法
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    
    /**
     * 操作名称
     */
    String operation() default "";
    
    /**
     * 操作描述
     */
    String description() default "";
    
    /**
     * 事件类型
     */
    AuditEventType eventType() default AuditEventType.BUSINESS_OPERATION;
    
    /**
     * 资源类型
     */
    String resourceType() default "";
    
    /**
     * 模块名称
     */
    String module() default "";
    
    /**
     * 是否记录参数
     */
    boolean includeParameters() default true;
    
    /**
     * 是否记录返回值
     */
    boolean includeResult() default false;
    
    /**
     * 是否记录异常信息
     */
    boolean includeException() default true;
    
    /**
     * 是否异步记录
     */
    boolean async() default true;
    
    /**
     * 敏感参数索引（这些参数将被脱敏）
     * @deprecated 推荐使用 sensitiveParamNames 或 sensitiveParamExpression
     */
    @Deprecated
    int[] sensitiveParams() default {};
    
    /**
     * 敏感参数名称（这些参数将被脱敏）
     * 示例：{"password", "creditCardNumber"}
     */
    String[] sensitiveParamNames() default {};
    
    /**
     * 敏感参数SpEL表达式
     * 用于复杂的敏感数据识别和脱敏逻辑
     * 示例："#request.password != null" 或 "#user.email.contains('@')"
     */
    String sensitiveParamExpression() default "";
    
    /**
     * 忽略的参数索引
     * @deprecated 推荐使用 ignoreParamNames
     */
    @Deprecated
    int[] ignoreParams() default {};
    
    /**
     * 忽略的参数名称
     * 示例：{"request", "response"}
     */
    String[] ignoreParamNames() default {};
    
    /**
     * 业务标识SpEL表达式
     * 例如：#user.id 或 #request.orderId
     */
    String businessKey() default "";
    
    /**
     * 条件SpEL表达式
     * 只有条件为true时才记录审计日志
     */
    String condition() default "";
    
    /**
     * 是否启用参数注解扫描
     * 当为true时，会扫描方法参数上的@SensitiveParam和@IgnoreParam注解
     */
    boolean enableParamAnnotations() default true;
}