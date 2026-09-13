package io.github.youngerier.support.audit.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要审计的方法或类。标注在类上时，该类的所有 public 方法都会被审计。
 *
 * <p>{@code businessKey} 与 {@code condition} 支持 SpEL，可直接引用方法参数名
 * （编译需开启 {@code -parameters}，本项目已开启），也可用 {@code #param0} 形式：
 * <pre>{@code
 * @Auditable(operation = "创建用户", businessKey = "#user.id")
 * public void create(User user) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * 操作名称，默认取方法名
     */
    String operation() default "";

    /**
     * 操作类型（如 LOGIN / CREATE / UPDATE），由使用方自定义
     */
    String type() default "";

    /**
     * 业务主键的 SpEL 表达式，例如 "#id"、"#user.id"
     */
    String businessKey() default "";

    /**
     * 记录条件的 SpEL 表达式，求值为 false 时跳过本次审计
     */
    String condition() default "";

    /**
     * 是否记录方法参数
     */
    boolean includeParameters() default true;

    /**
     * 是否记录方法返回值
     */
    boolean includeResult() default false;

    /**
     * 是否异步记录（默认异步，不阻塞业务线程）
     */
    boolean async() default true;
}
