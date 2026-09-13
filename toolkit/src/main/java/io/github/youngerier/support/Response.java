package io.github.youngerier.support;

import io.github.youngerier.support.exception.ExceptionCode;
import lombok.Data;

/**
 * 统一响应结构
 */
@Data
public class Response<T> {

    public static final int SUCCESS_CODE = 200;
    public static final String SUCCESS_MESSAGE = "success";
    public static final int ERROR_CODE = 500;

    private int code;
    private String message;
    private T data;

    private Response() {
    }

    private Response(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Response<T> ok(T data) {
        return new Response<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    public static <T> Response<T> ok() {
        return new Response<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    public static <T> Response<T> error(int code, String message) {
        return new Response<>(code, message, null);
    }

    public static <T> Response<T> error(String message) {
        return new Response<>(ERROR_CODE, message, null);
    }

    /**
     * 使用异常码构造错误响应，message 取异常码描述，数字码作为响应 code；
     * 非数字的业务码统一按 500 处理。
     */
    public static <T> Response<T> error(ExceptionCode exceptionCode) {
        return new Response<>(toHttpCode(exceptionCode.getCode()), exceptionCode.getDesc(), null);
    }

    private static int toHttpCode(String code) {
        try {
            int value = Integer.parseInt(code);
            return value >= 400 && value < 600 ? value : ERROR_CODE;
        } catch (NumberFormatException e) {
            return ERROR_CODE;
        }
    }

    public boolean isOk() {
        return code == SUCCESS_CODE;
    }

    public boolean isError() {
        return !isOk();
    }
}
