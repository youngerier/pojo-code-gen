package io.github.youngerier.support.audit.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记审计时需要脱敏的方法参数。
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
     * 自定义脱敏 SpEL 表达式（strategy = CUSTOM 时生效），参数值通过 #value 访问，
     * 例如 {@code #value.substring(0, 3) + '***'}
     */
    String customExpression() default "";

    /**
     * 脱敏策略
     */
    enum MaskStrategy {
        /** 保留前2后2 */
        DEFAULT,
        /** 全部掩码 */
        FULL,
        /** 邮箱 */
        EMAIL,
        /** 手机号，保留前3后4 */
        PHONE,
        /** 银行卡，保留后4位 */
        BANK_CARD,
        /** 身份证，保留前4后4 */
        ID_CARD,
        /** SpEL 自定义 */
        CUSTOM
    }
}
