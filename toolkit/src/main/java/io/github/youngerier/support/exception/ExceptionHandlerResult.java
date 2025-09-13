package io.github.youngerier.support.exception;

import io.github.youngerier.support.ExceptionUtils;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 异常处理结果封装
 * 统一异常处理的返回格式，提供丰富的错误信息
 * 
 * 设计原则：
 * 1. 信息完整：包含错误码、消息、HTTP状态码等完整信息
 * 2. 结构统一：所有异常处理都返回相同结构的结果
 * 3. 便于监控：包含时间戳、追踪ID等便于问题排查的信息
 * 4. 用户友好：提供用户可读的错误消息
 */
@Data
@Builder(toBuilder = true)
public class ExceptionHandlerResult {

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误消息（用户友好）
     */
    private String message;

    /**
     * 详细错误信息（开发调试用）
     */
    private String details;

    /**
     * HTTP状态码
     */
    private HttpStatus httpStatus;

    /**
     * 异常发生时间
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 请求路径
     */
    private String path;

    /**
     * 追踪ID（便于日志关联）
     */
    private String traceId;

    /**
     * 额外的错误信息
     */
    private Map<String, Object> extra;

    /**
     * 是否应该记录错误日志
     */
    @Builder.Default
    private boolean shouldLog = true;

    /**
     * 日志级别
     */
    @Builder.Default
    private LogLevel logLevel = LogLevel.ERROR;

    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    /**
     * 创建业务异常结果
     */
    public static ExceptionHandlerResult business(String errorCode, String message) {
        return ExceptionHandlerResult.builder()
                .errorCode(errorCode)
                .message(message)
                .httpStatus(HttpStatus.OK)
                .logLevel(LogLevel.WARN)
                .build();
    }

    /**
     * 创建业务异常结果（自动国际化）
     */
    public static ExceptionHandlerResult business(I18nExceptionCode exceptionCode) {
        return business(exceptionCode.getCode(), ExceptionUtils.getLocalizedMessage(exceptionCode));
    }

    /**
     * 创建业务异常结果（带参数，自动国际化）
     */
    public static ExceptionHandlerResult business(I18nExceptionCode exceptionCode, Object... args) {
        return business(exceptionCode.getCode(), ExceptionUtils.getLocalizedMessage(exceptionCode, args));
    }

    /**
     * 创建系统异常结果
     */
    public static ExceptionHandlerResult system(String errorCode, String message) {
        return ExceptionHandlerResult.builder()
                .errorCode(errorCode)
                .message(message)
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .logLevel(LogLevel.ERROR)
                .build();
    }

    /**
     * 创建系统异常结果（自动国际化）
     */
    public static ExceptionHandlerResult system(I18nExceptionCode exceptionCode) {
        return system(exceptionCode.getCode(), ExceptionUtils.getLocalizedMessage(exceptionCode));
    }

    /**
     * 创建系统异常结果（带参数，自动国际化）
     */
    public static ExceptionHandlerResult system(I18nExceptionCode exceptionCode, Object... args) {
        return system(exceptionCode.getCode(), ExceptionUtils.getLocalizedMessage(exceptionCode, args));
    }

    /**
     * 创建参数验证异常结果
     */
    public static ExceptionHandlerResult validation(String errorCode, String message) {
        return ExceptionHandlerResult.builder()
                .errorCode(errorCode)
                .message(message)
                .httpStatus(HttpStatus.BAD_REQUEST)
                .logLevel(LogLevel.WARN)
                .build();
    }

    /**
     * 创建参数验证异常结果（自动国际化）
     */
    public static ExceptionHandlerResult validation(I18nExceptionCode exceptionCode) {
        return validation(exceptionCode.getCode(), ExceptionUtils.getLocalizedMessage(exceptionCode));
    }

    /**
     * 创建参数验证异常结果（带参数，自动国际化）
     */
    public static ExceptionHandlerResult validation(I18nExceptionCode exceptionCode, Object... args) {
        return validation(exceptionCode.getCode(), ExceptionUtils.getLocalizedMessage(exceptionCode, args));
    }

    /**
     * 创建认证异常结果
     */
    public static ExceptionHandlerResult authentication(String errorCode, String message) {
        return ExceptionHandlerResult.builder()
                .errorCode(errorCode)
                .message(message)
                .httpStatus(HttpStatus.UNAUTHORIZED)
                .logLevel(LogLevel.WARN)
                .build();
    }

    /**
     * 创建认证异常结果（自动国际化）
     */
    public static ExceptionHandlerResult authentication(I18nExceptionCode exceptionCode) {
        return authentication(exceptionCode.getCode(), ExceptionUtils.getLocalizedMessage(exceptionCode));
    }

    /**
     * 创建认证异常结果（带参数，自动国际化）
     */
    public static ExceptionHandlerResult authentication(I18nExceptionCode exceptionCode, Object... args) {
        return authentication(exceptionCode.getCode(), ExceptionUtils.getLocalizedMessage(exceptionCode, args));
    }

    /**
     * 创建授权异常结果
     */
    public static ExceptionHandlerResult authorization(String errorCode, String message) {
        return ExceptionHandlerResult.builder()
                .errorCode(errorCode)
                .message(message)
                .httpStatus(HttpStatus.FORBIDDEN)
                .logLevel(LogLevel.WARN)
                .build();
    }

    /**
     * 创建授权异常结果（自动国际化）
     */
    public static ExceptionHandlerResult authorization(I18nExceptionCode exceptionCode) {
        return authorization(exceptionCode.getCode(), ExceptionUtils.getLocalizedMessage(exceptionCode));
    }

    /**
     * 创建授权异常结果（带参数，自动国际化）
     */
    public static ExceptionHandlerResult authorization(I18nExceptionCode exceptionCode, Object... args) {
        return authorization(exceptionCode.getCode(), ExceptionUtils.getLocalizedMessage(exceptionCode, args));
    }
}