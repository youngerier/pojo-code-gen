package io.github.youngerier.support.i18n;

import java.util.Locale;

/**
 * 异常消息模板来源 SPI。实现并注册为 Spring Bean 即可从任意来源加载消息模板，
 * 典型场景是从数据库字典表加载（建议实现内加缓存）。
 *
 * <p>实现只需返回模板原文，占位符统一使用 slf4j 风格的 {@code {}}（与
 * {@code BaseException.notFound("用户{}不存在", id)} 一致），参数替换由
 * {@link ExceptionMessageResolver} 通过 {@code MessageFormatter} 统一完成。
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
 *     @Cacheable("exception-messages")
 *     public String getMessage(String key, Locale locale) {
 *         return mapper.findTemplate(key, locale.toLanguageTag()); // 例如 "用户 {} 不存在"；查不到返回 null
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface ExceptionMessageProvider {

    /**
     * 加载消息模板（包含 {@code {}} 占位符的原文）。
     *
     * @param key    消息键，如 exception.user_not_found
     * @param locale 请求语言
     * @return 消息模板；该来源无对应消息时返回 null，继续尝试后续来源
     */
    String getMessage(String key, Locale locale);
}
