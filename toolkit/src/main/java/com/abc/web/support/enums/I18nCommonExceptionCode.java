package com.abc.web.support.enums;

import com.abc.web.support.exception.I18nExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 国际化通用异常码枚举
 */
@AllArgsConstructor
@Getter
public enum I18nCommonExceptionCode implements I18nExceptionCode {

    // 成功
    SUCCESS("0", "exception.success", "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST("400", "exception.bad_request", "请求参数错误"),
    UNAUTHORIZED("401", "exception.unauthorized", "未授权访问"),
    FORBIDDEN("403", "exception.forbidden", "禁止访问"),
    NOT_FOUND("404", "exception.not_found", "资源不存在"),
    METHOD_NOT_ALLOWED("405", "exception.method_not_allowed", "请求方法不允许"),
    CONFLICT("409", "exception.conflict", "资源冲突"),
    VALIDATION_ERROR("422", "exception.validation_error", "数据验证失败"),

    // 服务端错误 5xx
    INTERNAL_SERVER_ERROR("500", "exception.internal_server_error", "服务器内部错误"),
    NOT_IMPLEMENTED("501", "exception.not_implemented", "功能未实现"),
    SERVICE_UNAVAILABLE("503", "exception.service_unavailable", "服务不可用"),

    // 业务错误 1xxx
    BUSINESS_ERROR("1000", "exception.business_error", "业务处理失败"),
    DATA_NOT_FOUND("1001", "exception.data_not_found", "数据不存在"),
    DATA_ALREADY_EXISTS("1002", "exception.data_already_exists", "数据已存在"),
    OPERATION_NOT_ALLOWED("1003", "exception.operation_not_allowed", "操作不被允许"),
    INSUFFICIENT_PERMISSIONS("1004", "exception.insufficient_permissions", "权限不足"),

    // 参数验证错误 1100-1199
    PARAM_REQUIRED("1100", "exception.param_required", "参数 {0} 不能为空"),
    PARAM_INVALID("1101", "exception.param_invalid", "参数 {0} 格式不正确"),
    PARAM_OUT_OF_RANGE("1102", "exception.param_out_of_range", "参数 {0} 超出范围 [{1}, {2}]"),
    EMAIL_INVALID("1103", "exception.email_invalid", "邮箱格式不正确"),
    PHONE_INVALID("1104", "exception.phone_invalid", "手机号格式不正确"),
    ID_CARD_INVALID("1105", "exception.id_card_invalid", "身份证号格式不正确"),

    // 用户相关错误 1200-1299
    USER_NOT_FOUND("1200", "exception.user_not_found", "用户不存在"),
    USER_ALREADY_EXISTS("1201", "exception.user_already_exists", "用户已存在"),
    USER_DISABLED("1202", "exception.user_disabled", "用户已被禁用"),
    PASSWORD_INCORRECT("1203", "exception.password_incorrect", "密码错误"),
    PASSWORD_EXPIRED("1204", "exception.password_expired", "密码已过期"),

    // 数据库错误 2xxx
    DATABASE_ERROR("2000", "exception.database_error", "数据库操作失败"),
    DUPLICATE_KEY_ERROR("2001", "exception.duplicate_key_error", "数据重复"),
    FOREIGN_KEY_ERROR("2002", "exception.foreign_key_error", "外键约束错误"),
    DATA_INTEGRITY_ERROR("2003", "exception.data_integrity_error", "数据完整性错误"),

    // 外部服务错误 3xxx
    EXTERNAL_SERVICE_ERROR("3000", "exception.external_service_error", "外部服务调用失败"),
    NETWORK_ERROR("3001", "exception.network_error", "网络连接错误"),
    TIMEOUT_ERROR("3002", "exception.timeout_error", "请求超时"),

    // 文件操作错误 4xxx
    FILE_NOT_FOUND("4000", "exception.file_not_found", "文件不存在"),
    FILE_UPLOAD_ERROR("4001", "exception.file_upload_error", "文件上传失败"),
    FILE_FORMAT_ERROR("4002", "exception.file_format_error", "文件格式错误"),
    FILE_SIZE_EXCEEDED("4003", "exception.file_size_exceeded", "文件大小超出限制 {0}MB");

    private final String code;
    private final String messageKey;
    private final String desc;

    @Override
    public String getDefaultMessage() {
        return desc;
    }

    /**
     * 根据错误码查找异常码
     *
     * @param code 错误码
     * @return 异常码，如果未找到返回 INTERNAL_SERVER_ERROR
     */
    public static I18nCommonExceptionCode fromCode(String code) {
        for (I18nCommonExceptionCode exceptionCode : values()) {
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