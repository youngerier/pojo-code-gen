package com.abc.web.support.audit.annotations;

import java.lang.annotation.*;

/**
 * 忽略参数注解
 * 用于标记方法参数中不需要记录到审计日志的参数
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreParam {
    
    /**
     * 忽略原因说明
     */
    String reason() default "";
}