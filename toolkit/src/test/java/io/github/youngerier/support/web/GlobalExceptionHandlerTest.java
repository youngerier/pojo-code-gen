package io.github.youngerier.support.web;

import io.github.youngerier.support.Response;
import io.github.youngerier.support.exception.BaseException;
import io.github.youngerier.support.exception.DefaultExceptionCode;
import io.github.youngerier.support.exception.ExceptionCode;
import io.github.youngerier.support.exception.ExceptionLogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(null);
    }

    @Test
    void baseExceptionMapsCodeToHttpStatusAndKeepsMessage() {
        ResponseEntity<Response<Void>> response =
                handler.handleBaseException(BaseException.notFound("用户不存在"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getCode());
        assertEquals("用户不存在", response.getBody().getMessage());
    }

    @Test
    void forbiddenWithLevelStillUsesForbiddenCode() {
        // 回归：forbidden(level, message) 曾误用 COMMON_ERROR
        ResponseEntity<Response<Void>> response =
                handler.handleBaseException(BaseException.forbidden(ExceptionLogLevel.WARN, "禁止访问"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getCode());
    }

    @Test
    void friendlyExceptionHidesInternalMessage() {
        ResponseEntity<Response<Void>> response =
                handler.handleBaseException(BaseException.friendly("内部细节：xxx"));

        assertEquals(500, response.getBody().getCode());
        assertEquals(DefaultExceptionCode.INTERNAL_SERVER_ERROR.getDesc(), response.getBody().getMessage());
    }

    @Test
    void businessCodeMapsTo422AndKeepsRawCodeInBody() {
        ExceptionCode businessCode = new ExceptionCode() {
            @Override
            public String getCode() {
                return "10001";
            }

            @Override
            public String getDesc() {
                return "账号已禁用";
            }
        };

        ResponseEntity<Response<Void>> response =
                handler.handleBaseException(BaseException.business(businessCode, "账号已禁用"));

        assertEquals(422, response.getStatusCode().value());
        assertEquals(10001, response.getBody().getCode());
        assertEquals("账号已禁用", response.getBody().getMessage());
    }

    @Test
    void bindExceptionReturns400WithFieldMessages() {
        BindException bindException = new BindException(new Object(), "request");
        bindException.addError(new FieldError("request", "username", "不能为空"));

        ResponseEntity<Response<Void>> response = handler.handleBindException(bindException);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
        assertEquals("username: 不能为空", response.getBody().getMessage());
    }

    @Test
    void unexpectedExceptionReturns500WithoutLeakingMessage() {
        ResponseEntity<Response<Void>> response =
                handler.handleException(new RuntimeException("数据库密码是 secret"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getCode());
        assertEquals(DefaultExceptionCode.INTERNAL_SERVER_ERROR.getDesc(), response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }
}
