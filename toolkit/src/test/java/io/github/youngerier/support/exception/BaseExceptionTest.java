package io.github.youngerier.support.exception;

import io.github.youngerier.support.message.MessagePlaceholder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BaseExceptionTest {

    @Test
    void factoryMethodsAttachExpectedCodes() {
        assertEquals("400", BaseException.badRequest("bad").getTextCode());
        assertEquals("401", BaseException.unAuthorized("no").getTextCode());
        assertEquals("403", BaseException.forbidden("no").getTextCode());
        assertEquals("404", BaseException.notFound("missing").getTextCode());
    }

    @Test
    void forbiddenWithLevelKeepsForbiddenCode() {
        // 回归：曾误用 COMMON_ERROR
        BaseException ex = BaseException.forbidden(ExceptionLogLevel.WARN, "no");
        assertEquals(DefaultExceptionCode.FORBIDDEN, ex.getCode());
        assertEquals(ExceptionLogLevel.WARN, ex.getLogLevel());
    }

    @Test
    void friendlyUsesFriendlyCode() {
        assertEquals(DefaultExceptionCode.COMMON_FRIENDLY_ERROR,
                BaseException.friendly("内部细节").getCode());
    }

    @Test
    void slf4jStylePlaceholdersAreResolved() {
        BaseException ex = BaseException.common(MessagePlaceholder.of("用户 {} 不存在", 42));
        assertEquals("用户 42 不存在", ex.getMessage());
    }

    @Test
    void causeIsPreserved() {
        Throwable cause = new IllegalStateException("root");
        BaseException ex = new BaseException(DefaultExceptionCode.COMMON_ERROR, "wrapped", cause);
        assertSame(cause, ex.getCause());
    }
}
