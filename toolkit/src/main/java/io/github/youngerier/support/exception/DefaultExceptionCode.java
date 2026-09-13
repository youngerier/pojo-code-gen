package io.github.youngerier.support.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 默认提供的通用异常
 *
 **/
@Getter
@AllArgsConstructor
public enum DefaultExceptionCode implements ExceptionCode {

    BAD_REQUEST("400", "请求不合法"),

    UNAUTHORIZED("401", "未认证"),

    FORBIDDEN("403", "无权限访问该资源"),

    PAYLOAD_TOO_LARGE("413", "上传文件大小超过限制"),

    TO_MANY_REQUESTS("429", "请求过于频繁"),

    NOT_FOUND("404", "资源不存在"),

    COMMON_ERROR("500", "通用业务错误"),

    /**
     * 不希望把内部错误信息直接暴露给用户时使用，
     * 全局异常处理器统一返回通用提示。
     */
    COMMON_FRIENDLY_ERROR("500", "业务异常，请稍后重试");

    private final String code;

    private final String desc;

}
