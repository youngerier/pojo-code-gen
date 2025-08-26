package com.abc.web.support;

import com.abc.web.support.enums.I18nCommonExceptionCode;
import com.abc.web.support.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 异常工具类
 * 纯粹的异常创建和抛出工具，不包含断言逻辑
 */
@Component
public class ExceptionUtils {

    private static I18nMessageService messageService;

    @Autowired
    public void setMessageService(I18nMessageService messageService) {
        ExceptionUtils.messageService = messageService;
    }

    // ==================== 业务异常 ====================

    /**
     * 创建业务异常
     */
    public static BusinessException business(I18nExceptionCode exceptionCode) {
        return new BusinessException(exceptionCode);
    }

    /**
     * 创建业务异常（带参数）
     */
    public static BusinessException business(I18nExceptionCode exceptionCode, Object... args) {
        return new BusinessException(exceptionCode, args);
    }

    /**
     * 抛出业务异常
     */
    public static void throwBusiness(I18nExceptionCode exceptionCode) {
        throw business(exceptionCode);
    }

    /**
     * 抛出业务异常（带参数）
     */
    public static void throwBusiness(I18nExceptionCode exceptionCode, Object... args) {
        throw business(exceptionCode, args);
    }

    /**
     * 条件抛出业务异常
     */
    public static void throwIf(boolean condition, I18nExceptionCode exceptionCode) {
        if (condition) {
            throwBusiness(exceptionCode);
        }
    }

    /**
     * 条件抛出业务异常（带参数）
     */
    public static void throwIf(boolean condition, I18nExceptionCode exceptionCode, Object... args) {
        if (condition) {
            throwBusiness(exceptionCode, args);
        }
    }

    // ==================== 系统异常 ====================

    /**
     * 创建系统异常
     */
    public static SystemException system(I18nExceptionCode exceptionCode) {
        return new SystemException(exceptionCode);
    }

    /**
     * 创建系统异常（带原因）
     */
    public static SystemException system(I18nExceptionCode exceptionCode, Throwable cause) {
        return new SystemException(exceptionCode, cause);
    }

    /**
     * 抛出系统异常
     */
    public static void throwSystem(I18nExceptionCode exceptionCode) {
        throw system(exceptionCode);
    }

    /**
     * 抛出系统异常（带原因）
     */
    public static void throwSystem(I18nExceptionCode exceptionCode, Throwable cause) {
        throw system(exceptionCode, cause);
    }

    // ==================== 便捷方法 ====================

    /**
     * 数据不存在异常
     */
    public static void throwNotFound() {
        throwBusiness(I18nCommonExceptionCode.DATA_NOT_FOUND);
    }

    /**
     * 数据已存在异常
     */
    public static void throwAlreadyExists() {
        throwBusiness(I18nCommonExceptionCode.DATA_ALREADY_EXISTS);
    }

    /**
     * 未授权异常
     */
    public static void throwUnauthorized() {
        throwBusiness(I18nCommonExceptionCode.UNAUTHORIZED);
    }

    /**
     * 权限不足异常
     */
    public static void throwForbidden() {
        throwBusiness(I18nCommonExceptionCode.FORBIDDEN);
    }

    // ==================== 消息处理 ====================

    /**
     * 获取异常的国际化消息
     */
    public static String getLocalizedMessage(BaseException exception) {
        if (messageService == null) {
            return exception.getMessage();
        }

        ExceptionCode exceptionCode = exception.getExceptionCode();
        if (exceptionCode instanceof I18nExceptionCode) {
            return messageService.getExceptionMessage(
                    (I18nExceptionCode) exceptionCode,
                    exception.getArgs()
            );
        }

        return exception.getMessage();
    }

    /**
     * 获取异常的国际化消息
     */
    public static String getLocalizedMessage(I18nExceptionCode exceptionCode, Object... args) {
        if (messageService == null) {
            return exceptionCode.getDefaultMessage();
        }
        return messageService.getExceptionMessage(exceptionCode, args);
    }
}