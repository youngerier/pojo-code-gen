package com.abc.web.support.exception;

import lombok.Getter;

/**
 * 基础异常类
 * 所有业务异常的父类，支持国际化
 */
@Getter
public class BaseException extends RuntimeException {

    /**
     * 异常码
     */
    private final ExceptionCode exceptionCode;

    /**
     * 国际化参数
     */
    private final Object[] args;

    /**
     * 构造函数
     *
     * @param exceptionCode 异常码
     */
    public BaseException(ExceptionCode exceptionCode) {
        this(exceptionCode, null, null);
    }

    /**
     * 构造函数
     *
     * @param exceptionCode 异常码
     * @param args          国际化参数
     */
    public BaseException(ExceptionCode exceptionCode, Object[] args) {
        this(exceptionCode, args, null);
    }

    /**
     * 构造函数
     *
     * @param exceptionCode 异常码
     * @param cause         原因异常
     */
    public BaseException(ExceptionCode exceptionCode, Throwable cause) {
        this(exceptionCode, null, cause);
    }

    /**
     * 构造函数
     *
     * @param exceptionCode 异常码
     * @param args          国际化参数
     * @param cause         原因异常
     */
    public BaseException(ExceptionCode exceptionCode, Object[] args, Throwable cause) {
        super(exceptionCode.getDesc(), cause);
        this.exceptionCode = exceptionCode;
        this.args = args;
    }

    /**
     * 获取异常码字符串
     *
     * @return 异常码
     */
    public String getCode() {
        return exceptionCode.getCode();
    }

    /**
     * 获取异常描述
     *
     * @return 异常描述
     */
    public String getDesc() {
        return exceptionCode.getDesc();
    }

    /**
     * 是否有国际化参数
     *
     * @return true 如果有参数
     */
    public boolean hasArgs() {
        return args != null && args.length > 0;
    }
}