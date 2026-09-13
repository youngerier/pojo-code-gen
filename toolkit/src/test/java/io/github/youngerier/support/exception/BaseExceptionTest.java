package io.github.youngerier.support.exception;

import io.github.youngerier.support.message.MessagePlaceholder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseExceptionTest {

    @Test
    void factoryMethodsAttachExpectedCodes() {
        assertEquals("400", BaseException.badRequest("bad").getTextCode());
        assertEquals("401", BaseException.unauthorized("no").getTextCode());
        assertEquals("403", BaseException.forbidden("no").getTextCode());
        assertEquals("404", BaseException.notFound("missing").getTextCode());
        assertEquals("409", BaseException.conflict("dup").getTextCode());
        assertEquals("429", BaseException.tooManyRequests("slow").getTextCode());
        assertEquals("503", BaseException.serviceUnavailable("down").getTextCode());
    }

    @Test
    void forbiddenWithLevelKeepsForbiddenCode() {
        // 回归：曾误用 COMMON_ERROR
        BaseException ex = BaseException.forbidden(ExceptionLogLevel.WARN, "no");
        assertEquals(DefaultExceptionCode.FORBIDDEN, ex.getCode());
        assertEquals(ExceptionLogLevel.WARN, ex.getLogLevel());
    }

    @Test
    void defaultLogLevelFollowsStatusRange() {
        assertEquals(ExceptionLogLevel.WARN, BaseException.notFound("x").getLogLevel());
        assertEquals(ExceptionLogLevel.ERROR, BaseException.common("x").getLogLevel());
    }

    @Test
    void friendlyHidesMessageAndIsMarked() {
        BaseException ex = BaseException.friendly("内部细节");
        assertTrue(ex.isFriendly());
        assertEquals(DefaultExceptionCode.INTERNAL_SERVER_ERROR, ex.getCode());
        assertEquals("内部细节", ex.getMessage());
    }

    @Test
    void nonFriendlyByDefault() {
        assertFalse(BaseException.badRequest("参数错误").isFriendly());
    }

    @Test
    void slf4jStylePlaceholdersAreResolved() {
        BaseException ex = BaseException.common("用户 {} 不存在", 42);
        assertEquals("用户 42 不存在", ex.getMessage());
    }

    @Test
    void placeholderFactoryIsResolved() {
        BaseException ex = BaseException.common(MessagePlaceholder.of("用户 {} 不存在", 42));
        assertEquals("用户 42 不存在", ex.getMessage());
    }

    @Test
    void nullMessageFallsBackToCodeDescription() {
        BaseException ex = new BaseException(DefaultExceptionCode.BAD_REQUEST, (String) null);
        assertEquals(DefaultExceptionCode.BAD_REQUEST.getDesc(), ex.getMessage());
    }

    @Test
    void causeIsPreserved() {
        Throwable cause = new IllegalStateException("root");
        BaseException ex = new BaseException(DefaultExceptionCode.INTERNAL_SERVER_ERROR, "wrapped", cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void customBusinessCodeIsKept() {
        ExceptionCode businessCode = new ExceptionCode() {
            @Override
            public String getCode() {
                return "10001";
            }

            @Override
            public String getDesc() {
                return "账号已被禁用";
            }
        };
        BaseException ex = BaseException.business(businessCode, "用户已禁用");
        assertEquals("10001", ex.getTextCode());
        assertEquals(422, businessCode.httpStatus());
    }
}
