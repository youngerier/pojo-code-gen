package io.github.youngerier.support.exception;

/**
 * 国际化异常码接口
 * 扩展基础异常码，支持国际化消息键
 */
public interface I18nExceptionCode extends ExceptionCode {

    /**
     * 获取国际化消息键
     * 用于从国际化资源文件中获取本地化消息
     *
     * @return 国际化消息键
     */
    String getMessageKey();

    /**
     * 获取默认消息模板
     * 当国际化资源不可用时使用的默认消息
     *
     * @return 默认消息模板
     */
    default String getDefaultMessage() {
        return getDesc();
    }

    /**
     * 是否支持参数化消息
     * 消息模板中是否包含占位符
     *
     * @return true 如果支持参数化
     */
    default boolean isParameterized() {
        return getDefaultMessage().contains("{") && getDefaultMessage().contains("}");
    }
}