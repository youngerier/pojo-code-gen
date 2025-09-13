package io.github.youngerier.support;

import io.github.youngerier.support.exception.I18nExceptionCode;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * 国际化消息服务
 * 提供异常消息的国际化支持
 */
@Service
public class I18nMessageService {

    private final MessageSource messageSource;

    public I18nMessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 获取国际化消息
     *
     * @param messageKey 消息键
     * @return 国际化消息
     */
    public String getMessage(String messageKey) {
        return getMessage(messageKey, null, null);
    }

    /**
     * 获取国际化消息
     *
     * @param messageKey     消息键
     * @param args           参数
     * @return 国际化消息
     */
    public String getMessage(String messageKey, Object[] args) {
        return getMessage(messageKey, args, null);
    }

    /**
     * 获取国际化消息
     *
     * @param messageKey     消息键
     * @param args           参数
     * @param defaultMessage 默认消息
     * @return 国际化消息
     */
    public String getMessage(String messageKey, Object[] args, String defaultMessage) {
        return getMessage(messageKey, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * 获取国际化消息
     *
     * @param messageKey     消息键
     * @param args           参数
     * @param defaultMessage 默认消息
     * @param locale         语言环境
     * @return 国际化消息
     */
    public String getMessage(String messageKey, Object[] args, String defaultMessage, Locale locale) {
        try {
            return messageSource.getMessage(messageKey, args, defaultMessage, locale);
        } catch (Exception e) {
            // 如果获取国际化消息失败，返回默认消息
            return defaultMessage != null ? defaultMessage : messageKey;
        }
    }

    /**
     * 获取异常的国际化消息
     *
     * @param exceptionCode 异常码
     * @return 国际化消息
     */
    public String getExceptionMessage(I18nExceptionCode exceptionCode) {
        return getExceptionMessage(exceptionCode, null);
    }

    /**
     * 获取异常的国际化消息
     *
     * @param exceptionCode 异常码
     * @param args          参数
     * @return 国际化消息
     */
    public String getExceptionMessage(I18nExceptionCode exceptionCode, Object[] args) {
        return getMessage(
                exceptionCode.getMessageKey(),
                args,
                exceptionCode.getDefaultMessage()
        );
    }

    /**
     * 获取当前语言环境
     *
     * @return 当前语言环境
     */
    public Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    /**
     * 判断是否为中文环境
     *
     * @return true 如果是中文环境
     */
    public boolean isChinese() {
        Locale locale = getCurrentLocale();
        return Locale.CHINESE.getLanguage().equals(locale.getLanguage()) ||
               Locale.CHINA.equals(locale);
    }

    /**
     * 判断是否为英文环境
     *
     * @return true 如果是英文环境
     */
    public boolean isEnglish() {
        Locale locale = getCurrentLocale();
        return Locale.ENGLISH.getLanguage().equals(locale.getLanguage()) ||
               Locale.US.equals(locale);
    }
}