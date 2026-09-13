package io.github.youngerier.support.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 默认提供的通用异常码，code 与标准 HTTP 状态码对齐。
 * 业务异常码由各项目自定义枚举实现 {@link ExceptionCode}。
 */
@Getter
@AllArgsConstructor
public enum DefaultExceptionCode implements ExceptionCode {

    // ---------------- 客户端错误 4xx ----------------

    BAD_REQUEST("400", "请求不合法"),

    UNAUTHORIZED("401", "未认证或登录已过期"),

    FORBIDDEN("403", "无权限访问该资源"),

    NOT_FOUND("404", "资源不存在"),

    CONFLICT("409", "资源冲突"),

    PAYLOAD_TOO_LARGE("413", "上传文件大小超过限制"),

    TOO_MANY_REQUESTS("429", "请求过于频繁"),

    // ---------------- 服务端错误 5xx ----------------

    INTERNAL_SERVER_ERROR("500", "系统繁忙，请稍后重试"),

    SERVICE_UNAVAILABLE("503", "服务暂不可用");

    private final String code;

    private final String desc;
}
