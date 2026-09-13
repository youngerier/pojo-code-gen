package io.github.youngerier.support.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;

/**
 * 异常消息解析，按顺序尝试：
 * <ol>
 *     <li>应用自身的 {@link MessageSource}（如 messages.properties）</li>
 *     <li>所有 {@link ExceptionMessageProvider}（如数据库字典，按 Spring 顺序依次尝试）</li>
 *     <li>toolkit 内置的中英文异常消息 bundle</li>
 *     <li>调用方提供的默认文案</li>
 * </ol>
 */
public class ExceptionMessageResolver {

    private static final String BUNDLE_BASENAME = "io.github.youngerier.support.i18n.exception-messages";

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
     * @param args           MessageFormat 格式参数
     * @param locale         目标语言
     * @param defaultMessage 全部未命中时的兜底文案
     */
    public String resolve(String key, Object[] args, Locale locale, String defaultMessage) {
        if (applicationMessageSource != null) {
            String message = applicationMessageSource.getMessage(key, args, null, locale);
            if (message != null) {
                return message;
            }
        }
        for (ExceptionMessageProvider provider : providers) {
            String message = provider.getMessage(key, locale, args);
            if (message != null) {
                return message;
            }
        }
        return toolkitMessageSource.getMessage(key, args, defaultMessage, locale);
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
