package io.github.youngerier.support.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

/**
 * 基于配置文件（properties）的异常消息模板来源：按 locale 从 classpath 的 ResourceBundle
 * 加载模板，文件命名遵循 {@code basename.properties} / {@code basename_en.properties} 规则。
 *
 * <p>典型用法：在 {@code resources/} 下放置 {@code exception-messages.properties}（基础语言，建议中文）
 * 与 {@code exception-messages_en.properties}（英文），构造时传入 basename
 * {@code "exception-messages"}。模板中的 {@code {}} 占位符由 {@link ExceptionMessageResolver}
 * 统一替换，本类只返回模板原文；查不到时返回 {@code null}，继续尝试后续来源。
 *
 * <p>需要加载 classpath 外的文件或支持热更新时，可自行构造
 * {@link org.springframework.context.support.RefreshableResourceBundleMessageSource}（支持
 * {@code file:} 前缀）通过 {@link #ResourceExceptionMessageProvider(MessageSource)} 传入。
 */
public class ResourceExceptionMessageProvider implements ExceptionMessageProvider {

    private final MessageSource messageSource;

    /**
     * @param basenames ResourceBundle basename（不含 .properties 后缀），支持多个，
     *                  如 {@code "exception-messages"} 或 {@code "config/exception-messages"}
     */
    public ResourceExceptionMessageProvider(String... basenames) {
        this(buildMessageSource(basenames));
    }

    /**
     * @param messageSource 自定义消息源，如支持外部文件的 RefreshableResourceBundleMessageSource
     */
    public ResourceExceptionMessageProvider(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String getMessage(String key, Locale locale) {
        Locale target = locale != null ? locale : Locale.getDefault();
        // 未命中返回 null（不抛 NoSuchMessageException），交给解析链继续查找
        return messageSource.getMessage(key, null, null, target);
    }

    private static MessageSource buildMessageSource(String... basenames) {
        if (basenames == null || basenames.length == 0) {
            throw new IllegalArgumentException("at least one basename is required");
        }
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames(basenames);
        source.setDefaultEncoding("UTF-8");
        // 基础 bundle 为中文，请求语言（如德语）未命中时回退基础 bundle，而非系统语言
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
