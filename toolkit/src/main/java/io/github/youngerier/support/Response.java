package io.github.youngerier.support;

import io.github.youngerier.support.exception.ExceptionCode;
import lombok.Data;

@Data
public class Response<T> {
    private int code;
    private String message;
    private T data;

    private Response() {}

    private Response(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Response<T> ok(T data) {
        return new Response<>(200, "success", data);
    }

    public static <T> Response<T> ok() {
        return new Response<>(200, "success", null);
    }

    public static <T> Response<T> error(int code, String message) {
        return new Response<>(code, message, null);
    }

    public static <T> Response<T> error(String message) {
        return new Response<>(500, message, null);
    }

    public static <T> Response<T> error(ExceptionCode exceptionCode) {
        return new Response<>(Integer.parseInt(exceptionCode.getCode()), 
                             exceptionCode.getCode(), null);
    }

    public boolean isOk() {
        return code == 200;
    }

    public boolean isError() {
        return !isOk();
    }
}
