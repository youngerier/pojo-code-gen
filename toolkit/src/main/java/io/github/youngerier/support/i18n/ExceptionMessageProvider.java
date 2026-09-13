package io.github.youngerier.support.i18n;

import java.util.Locale;

/**
 * 异常消息来源 SPI。实现并注册为 Spring Bean 即可从任意来源加载国际化消息，
 * 典型场景是从数据库字典表加载（建议实现内加缓存）。
 *
 * <p>解析顺序：应用 {@code MessageSource} → 全部 {@code ExceptionMessageProvider}
 * → toolkit 内置中英文 bundle → 异常码 desc。
 *
 * <p>数据库实现示例：
 * <pre>{@code
 * @Component
 * @RequiredArgsConstructor
 * public class DbExceptionMessageProvider implements ExceptionMessageProvider {
 *
 *     private final ErrorMessageMapper mapper;
 *
 *     @Override
 *     public String getMessage(String key, Locale locale, Object... args) {
 *         String template = mapper.findMessage(key, locale.toLanguageTag()); // 查库（可用 @Cacheable）
 *         if (template == null) {
 *             return null;  // 返回 null 继续尝试后续来源
 *         }
 *         return args == null || args.length == 0 ? template
 *                 : new MessageFormat(template, locale).format(args);
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface ExceptionMessageProvider {

    /**
     * 加载并（按需）格式化消息。
     *
     * @param key    消息键，如 exception.user_not_found
     * @param locale 请求语言
     * @param args   MessageFormat 风格参数（{0}、{1}），无参数时为空数组
     * @return 已格式化的消息；该来源无对应消息时返回 null
     */
    String getMessage(String key, Locale locale, Object... args);
}
