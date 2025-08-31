package com.abc.web.support.audit.annotations;

import java.lang.annotation.*;

/**
 * 敏感字段注解
 * 用于标记对象中的敏感属性，在审计序列化时将被脱敏处理
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SensitiveField {
    
    /**
     * 脱敏策略
     */
    SensitiveParam.MaskStrategy strategy() default SensitiveParam.MaskStrategy.DEFAULT;
    
    /**
     * 自定义脱敏表达式（当strategy为CUSTOM时使用）
     * 支持SpEL表达式，字段值通过 #value 访问
     * 例如：#value.substring(0, 3) + '***'
     */
    String customExpression() default "";
    
    /**
     * 字段描述（用于审计日志）
     */
    String description() default "";
    
    /**
     * 是否启用嵌套脱敏
     * 当该字段是复杂对象时，是否递归处理其内部的@SensitiveField注解
     */
    boolean enableNested() default true;
}