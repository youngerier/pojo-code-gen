package io.github.youngerier.support.web;

import io.github.youngerier.support.Response;
import io.github.youngerier.support.exception.BaseException;
import io.github.youngerier.support.exception.DefaultExceptionCode;
import io.github.youngerier.support.exception.ExceptionCode;
import io.github.youngerier.support.exception.ExceptionLogLevel;
import io.github.youngerier.support.i18n.ExceptionMessageProvider;
import io.github.youngerier.support.i18n.ExceptionMessageResolver;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 全局异常处理器，把异常统一转换为 {@link Response} 响应体，
 * 并按 {@link ExceptionLogLevel} 分级记录日志。
 *
 * <p>消息解析顺序：应用 {@link MessageSource}（支持 {@code Accept-Language}）
 * -> toolkit 内置中英文 bundle -> 异常码描述/字面量消息。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ExceptionMessageResolver messageResolver;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this(messageSource, List.of());
    }

    public GlobalExceptionHandler(MessageSource messageSource, List<ExceptionMessageProvider> providers) {
        this.messageResolver = new ExceptionMessageResolver(messageSource, providers);
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Response<Void>> handleBaseException(BaseException ex) {
        ExceptionCode code = ex.getCode();
        logByLevel(ex.getLogLevel(), ex);

        String message;
        if (ex.isFriendly() || ex.isI18n()) {
            // 友好异常与 i18n 异常：按请求 Locale 解析消息键，找不到时回退异常码描述
            message = resolveCodeMessage(code, ex.getMessageArgs());
        } else {
            message = ex.getMessage();
        }
        return ResponseEntity.status(code.httpStatus())
                .body(Response.error(toIntCode(code), message));
    }

    /**
     * @RequestBody 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return badRequest(fieldErrorMessages(ex));
    }

    /**
     * 表单/对象绑定校验失败
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Response<Void>> handleBindException(BindException ex) {
        return badRequest(fieldErrorMessages(ex));
    }

    /**
     * 方法参数（@RequestParam / @PathVariable）校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Response<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return badRequest(message);
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Response<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return badRequest("缺少必填参数: " + ex.getParameterName());
    }

    /**
     * 参数类型不匹配，例如 ?id=abc 但 id 为 Long
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Response<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return badRequest("参数类型错误: " + ex.getName());
    }

    /**
     * 请求体不可读（JSON 格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return badRequest("请求体格式错误");
    }

    /**
     * 上传文件超过大小限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Response<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Response.error(HttpStatus.PAYLOAD_TOO_LARGE.value(),
                        resolveCodeMessage(DefaultExceptionCode.PAYLOAD_TOO_LARGE, null)));
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Response<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Response.error(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不支持: " + ex.getMethod()));
    }

    /**
     * 静态资源 / 接口不存在（Spring 6 抛出）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Response<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Response.error(HttpStatus.NOT_FOUND.value(),
                        resolveCodeMessage(DefaultExceptionCode.NOT_FOUND, null)));
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.error(resolveCodeMessage(DefaultExceptionCode.INTERNAL_SERVER_ERROR, null)));
    }

    private ResponseEntity<Response<Void>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Response.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    private String fieldErrorMessages(BindException ex) {
        Locale locale = LocaleContextHolder.getLocale();
        return ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    // 校验注解 message 可写为消息键，由 MessageSource 解析；解析不了返回注解原文
                    String resolved = messageResolverRaw(error.getDefaultMessage(), locale);
                    return error.getField() + ": " + resolved;
                })
                .collect(Collectors.joining("; "));
    }

    private String resolveCodeMessage(ExceptionCode code, Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        String key = code.getMessageKey();
        if (key != null) {
            return messageResolver.resolve(key, args, locale, code.getDesc());
        }
        return code.getDesc();
    }

    private String messageResolverRaw(String key, Locale locale) {
        if (key == null) {
            return null;
        }
        // 仅当消息确实存在时解析，避免把普通中文提示当成消息键
        return messageResolver.resolve(key, null, locale, key);
    }

    /**
     * 业务异常默认 WARN 且不打印堆栈，只有显式 ERROR 级别才记录完整堆栈
     */
    private void logByLevel(ExceptionLogLevel level, BaseException ex) {
        switch (level) {
            case NONE -> {
            }
            case INFO -> log.info("Business exception: {}", ex.getMessage());
            case WARN -> log.warn("Business exception: {}", ex.getMessage());
            case ERROR -> log.error("Business exception", ex);
        }
    }

    private int toIntCode(ExceptionCode code) {
        try {
            return Integer.parseInt(code.getCode());
        } catch (NumberFormatException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
    }
}
