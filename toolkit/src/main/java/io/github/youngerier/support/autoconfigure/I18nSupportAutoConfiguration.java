package io.github.youngerier.support.autoconfigure;

import io.github.youngerier.support.i18n.SpringI18nMessageUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.annotation.Bean;

/**
 * 把容器中的 {@link MessageSource} 与请求 Locale 接入 {@link SpringI18nMessageUtils}，
 * 让静态工具类在应用启动后即可解析 {@code $.} 前缀的国际化消息。
 */
@AutoConfiguration
public class I18nSupportAutoConfiguration {

    @Bean
    public SmartInitializingSingleton i18nInitializer(ObjectProvider<MessageSource> messageSourceProvider) {
        return () -> {
            MessageSource messageSource = messageSourceProvider.getIfAvailable();
            if (messageSource != null) {
                SpringI18nMessageUtils.setMessageSource(messageSource);
            }
            SpringI18nMessageUtils.setLocaleSupplier(LocaleContextHolder::getLocale);
        };
    }
}
