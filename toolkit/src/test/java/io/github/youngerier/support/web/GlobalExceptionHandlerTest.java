package io.github.youngerier.support.web;

import io.github.youngerier.support.Response;
import io.github.youngerier.support.exception.BaseException;
import io.github.youngerier.support.exception.DefaultExceptionCode;
import io.github.youngerier.support.exception.ExceptionCode;
import io.github.youngerier.support.exception.ExceptionLogLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        // 固定 locale：消息解析走 LocaleContextHolder（默认取 JVM 默认语言），
        // 不固定会让断言依赖运行机器的语言环境
        LocaleContextHolder.setLocale(Locale.CHINESE);
        handler = new GlobalExceptionHandler(null);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
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

    // ---------------- 回归：兜底处理器不得吞掉语义正确的状态码 ----------------

    /**
     * 回归：{@code @ExceptionHandler(Exception.class)} 的优先级高于
     * ResponseStatusExceptionResolver，消费方 throw new ResponseStatusException(CONFLICT) 曾被吞成 500。
     */
    @Test
    void responseStatusExceptionKeepsItsStatusCodeAndReason() {
        ResponseEntity<Response<Void>> response =
                handler.handleException(new ResponseStatusException(HttpStatus.CONFLICT, "名称已存在"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getCode());
        assertEquals("名称已存在", response.getBody().getMessage());
    }

    @Test
    void responseStatusExceptionWithoutReasonFallsBackToMappedMessage() {
        ResponseEntity<Response<Void>> response =
                handler.handleException(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(DefaultExceptionCode.NOT_FOUND.getDesc(), response.getBody().getMessage());
    }

    @Test
    void httpMediaTypeNotSupportedKeeps415() {
        ResponseEntity<Response<Void>> response = handler.handleException(
                new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_XML, List.of(MediaType.APPLICATION_JSON)));

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertEquals(415, response.getBody().getCode());
    }

    @Test
    void asyncRequestTimeoutKeeps503() {
        ResponseEntity<Response<Void>> response = handler.handleException(new AsyncRequestTimeoutException());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(503, response.getBody().getCode());
    }

    /**
     * 回归：405 必须带 Allow 头（RFC 7231）。Spring 的 DefaultHandlerExceptionResolver 会设置，
     * 但本处理器优先级更高，需自己补上。
     */
    @Test
    void methodNotSupportedIncludesAllowHeader() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("PATCH", List.of("GET", "POST"));

        ResponseEntity<Response<Void>> response = handler.handleMethodNotSupported(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        Set<HttpMethod> allow = response.getHeaders().getAllow();
        assertTrue(allow != null && allow.containsAll(List.of(HttpMethod.GET, HttpMethod.POST)),
                "405 响应必须携带 Allow 头，实际为 " + allow);
    }
}
