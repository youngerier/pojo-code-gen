package io.github.youngerier.support.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceExceptionMessageProviderTest {

    private static final String BASENAME = "i18n/provider/exception-messages";
    private static final String EXTRA_BASENAME = "i18n/provider/extra-messages";

    @Test
    void loadsTemplateFromBaseBundleForChineseLocale() {
        ResourceExceptionMessageProvider provider = new ResourceExceptionMessageProvider(BASENAME);

        assertEquals("测试消息{}", provider.getMessage("exception.test", Locale.CHINESE));
    }

    @Test
    void loadsTemplateFromLanguageBundleForEnglish() {
        ResourceExceptionMessageProvider provider = new ResourceExceptionMessageProvider(BASENAME);

        assertEquals("test message{}", provider.getMessage("exception.test", Locale.ENGLISH));
    }

    @Test
    void fallsBackToBaseBundleForUnsupportedLanguage() {
        ResourceExceptionMessageProvider provider = new ResourceExceptionMessageProvider(BASENAME);

        // 德语 bundle 不存在，回退基础 bundle（而非系统语言）
        assertEquals("仅基础语言", provider.getMessage("exception.base_only", Locale.GERMAN));
    }

    @Test
    void returnsNullWhenKeyMissing() {
        ResourceExceptionMessageProvider provider = new ResourceExceptionMessageProvider(BASENAME);

        assertNull(provider.getMessage("exception.not_exists", Locale.CHINESE));
    }

    @Test
    void supportsMultipleBasenames() {
        ResourceExceptionMessageProvider provider =
                new ResourceExceptionMessageProvider(BASENAME, EXTRA_BASENAME);

        assertEquals("测试消息{}", provider.getMessage("exception.test", Locale.CHINESE));
        assertEquals("额外消息{}", provider.getMessage("exception.extra", Locale.CHINESE));
    }

    @Test
    void acceptsCustomMessageSourceDelegate() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("exception.custom", Locale.CHINESE, "自定义消息{}");
        ResourceExceptionMessageProvider provider = new ResourceExceptionMessageProvider(messageSource);

        assertEquals("自定义消息{}", provider.getMessage("exception.custom", Locale.CHINESE));
        assertNull(provider.getMessage("exception.custom", Locale.ENGLISH));
    }

    @Test
    void nullLocaleFallsBackToDefaultLocale() {
        ResourceExceptionMessageProvider provider = new ResourceExceptionMessageProvider(BASENAME);

        // 不抛异常；存在的 key 按 JVM 默认 locale 解析到基础或语言 bundle
        assertEquals("仅基础语言", provider.getMessage("exception.base_only", null));
        assertNull(provider.getMessage("exception.not_exists", null));
    }

    @Test
    void rejectsEmptyBasenames() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceExceptionMessageProvider());
    }
}
