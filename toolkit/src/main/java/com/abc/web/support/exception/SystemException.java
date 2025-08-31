package com.abc.web.support.exception;

/**
 * 系统异常
 * 用于系统级别的异常，如数据库连接失败、外部服务调用失败等
 */
public class SystemException extends BaseException {

    public SystemException(ExceptionCode exceptionCode) {
        super(exceptionCode);
    }

    public SystemException(ExceptionCode exceptionCode, Object[] args) {
        super(exceptionCode, args);
    }

    public SystemException(ExceptionCode exceptionCode, Throwable cause) {
        super(exceptionCode, cause);
    }

    public SystemException(ExceptionCode exceptionCode, Object[] args, Throwable cause) {
        super(exceptionCode, args, cause);
    }
}