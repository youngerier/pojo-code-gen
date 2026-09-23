package io.github.youngerier.support.i18n;

import io.github.youngerier.support.message.MessageFormatter;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;

/**
 * 异常消息解析，按顺序查找消息模板：
 * <ol>
 *     <li>应用自身的 {@link MessageSource}（如 messages.properties）</li>
 *     <li>所有 {@link ExceptionMessageProvider}（如数据库字典）</li>
 *     <li>toolkit 内置的中英文异常消息 bundle</li>
 *     <li>调用方提供的默认文案</li>
 * </ol>
 * 模板中的占位符统一使用 slf4j 风格的 {@code {}}（与 {@code BaseException.notFound("用户{}不存在", id)}
 * 一致），由 {@link MessageFormatter} 完成参数替换，各消息来源只负责返回模板原文。
 */
public class ExceptionMessageResolver {

    private static final String BUNDLE_BASENAME = "io.github.youngerier.support.i18n.exception-messages";

    private static final MessageFormatter MESSAGE_FORMATTER = MessageFormatter.slf4j();

    private final MessageSource applicationMessageSource;

    private final List<ExceptionMessageProvider> providers;

    private final MessageSource toolkitMessageSource = toolkitMessageSource();

    public ExceptionMessageResolver(MessageSource applicationMessageSource) {
        this(applicationMessageSource, List.of());
    }

    public ExceptionMessageResolver(MessageSource applicationMessageSource,
                                    List<ExceptionMessageProvider> providers) {
        this.applicationMessageSource = applicationMessageSource;
        this.providers = providers;
    }

    /**
     * @param key            消息键
     * @param args           占位符参数（{} 风格，按顺序替换）
     * @param locale         目标语言
     * @param defaultMessage 全部来源未命中时的兜底模板
     */
    public String resolve(String key, Object[] args, Locale locale, String defaultMessage) {
        String template = lookupTemplate(key, locale, defaultMessage);
        return format(template, args);
    }

    private String lookupTemplate(String key, Locale locale, String defaultMessage) {
        if (applicationMessageSource != null) {
            try {
                // 只取模板原文，参数替换统一交给 MessageFormatter
                String message = applicationMessageSource.getMessage(key, null, locale);
                if (message != null) {
                    return message;
                }
            } catch (NoSuchMessageException ignored) {
                // 继续尝试后续来源
            }
        }
        for (ExceptionMessageProvider provider : providers) {
            String message = provider.getMessage(key, locale);
            if (message != null) {
                return message;
            }
        }
        return toolkitMessageSource.getMessage(key, null, defaultMessage, locale);
    }

    private String format(String template, Object[] args) {
        if (template == null) {
            return null;
        }
        return args == null || args.length == 0 ? template : MESSAGE_FORMATTER.format(template, args);
    }

    private static MessageSource toolkitMessageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename(BUNDLE_BASENAME);
        source.setDefaultEncoding("UTF-8");
        // 基础 bundle 为中文，未命中英文等其他语言时回退中文
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
