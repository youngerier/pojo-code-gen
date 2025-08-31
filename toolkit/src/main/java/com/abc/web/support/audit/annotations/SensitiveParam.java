package com.abc.web.support.audit.annotations;

import java.lang.annotation.*;

/**
 * 敏感参数注解
 * 用于标记方法参数中的敏感数据，这些参数在审计时将被脱敏处理
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SensitiveParam {
    
    /**
     * 脱敏策略
     */
    MaskStrategy strategy() default MaskStrategy.DEFAULT;
    
    /**
     * 自定义脱敏表达式（当strategy为CUSTOM时使用）
     * 支持SpEL表达式，参数值通过 #value 访问
     * 例如：#value.substring(0, 3) + '***'
     */
    String customExpression() default "";
    
    /**
     * 参数描述（用于审计日志）
     */
    String description() default "";
    
    /**
     * 脱敏策略枚举
     */
    enum MaskStrategy {
        /**
         * 默认策略：保留前2位和后2位，中间用****代替
         */
        DEFAULT,
        
        /**
         * 全部脱敏：所有字符都用****代替
         */
        FULL,
        
        /**
         * 邮箱脱敏：保留@前1位和@后的域名
         */
        EMAIL,
        
        /**
         * 手机号脱敏：保留前3位和后4位
         */
        PHONE,
        
        /**
         * 银行卡脱敏：保留后4位
         */
        BANK_CARD,
        
        /**
         * 身份证脱敏：保留前4位和后4位
         */
        ID_CARD,
        
        /**
         * 自定义脱敏：使用customExpression
         */
        CUSTOM
    }
}