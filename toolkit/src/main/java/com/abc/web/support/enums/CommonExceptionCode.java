package com.abc.web.support.enums;

import com.abc.web.support.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用异常码枚举
 */
@AllArgsConstructor
@Getter
public enum CommonExceptionCode implements ExceptionCode {

    // 成功
    SUCCESS("0", "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST("400", "请求参数错误"),
    UNAUTHORIZED("401", "未授权访问"),
    FORBIDDEN("403", "禁止访问"),
    NOT_FOUND("404", "资源不存在"),
    METHOD_NOT_ALLOWED("405", "请求方法不允许"),
    CONFLICT("409", "资源冲突"),
    VALIDATION_ERROR("422", "数据验证失败"),

    // 服务端错误 5xx
    INTERNAL_SERVER_ERROR("500", "服务器内部错误"),
    NOT_IMPLEMENTED("501", "功能未实现"),
    SERVICE_UNAVAILABLE("503", "服务不可用"),

    // 业务错误 1xxx
    BUSINESS_ERROR("1000", "业务处理失败"),
    DATA_NOT_FOUND("1001", "数据不存在"),
    DATA_ALREADY_EXISTS("1002", "数据已存在"),
    OPERATION_NOT_ALLOWED("1003", "操作不被允许"),
    INSUFFICIENT_PERMISSIONS("1004", "权限不足"),

    // 数据库错误 2xxx
    DATABASE_ERROR("2000", "数据库操作失败"),
    DUPLICATE_KEY_ERROR("2001", "数据重复"),
    FOREIGN_KEY_ERROR("2002", "外键约束错误"),
    DATA_INTEGRITY_ERROR("2003", "数据完整性错误"),

    // 外部服务错误 3xxx
    EXTERNAL_SERVICE_ERROR("3000", "外部服务调用失败"),
    NETWORK_ERROR("3001", "网络连接错误"),
    TIMEOUT_ERROR("3002", "请求超时"),

    // 文件操作错误 4xxx
    FILE_NOT_FOUND("4000", "文件不存在"),
    FILE_UPLOAD_ERROR("4001", "文件上传失败"),
    FILE_FORMAT_ERROR("4002", "文件格式错误"),
    FILE_SIZE_EXCEEDED("4003", "文件大小超出限制");

    private final String code;
    private final String desc;

    /**
     * 根据错误码查找异常码
     *
     * @param code 错误码
     * @return 异常码，如果未找到返回 INTERNAL_SERVER_ERROR
     */
    public static CommonExceptionCode fromCode(String code) {
        for (CommonExceptionCode exceptionCode : values()) {
            if (exceptionCode.getCode().equals(code)) {
                return exceptionCode;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }

    /**
     * 判断是否为成功码
     *
     * @return true 如果是成功码
     */
    public boolean isSuccess() {
        return SUCCESS.equals(this);
    }

    /**
     * 判断是否为客户端错误
     *
     * @return true 如果是客户端错误（4xx）
     */
    public boolean isClientError() {
        return code.startsWith("4");
    }

    /**
     * 判断是否为服务端错误
     *
     * @return true 如果是服务端错误（5xx）
     */
    public boolean isServerError() {
        return code.startsWith("5");
    }

    /**
     * 判断是否为业务错误
     *
     * @return true 如果是业务错误（1xxx）
     */
    public boolean isBusinessError() {
        return code.startsWith("1");
    }
}