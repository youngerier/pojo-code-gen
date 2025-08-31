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
     * 创建系统异常（带参数）
     */
    public static SystemException system(I18nExceptionCode exceptionCode, Object... args) {
        return new SystemException(exceptionCode, args);
    }

    /**
     * 创建系统异常（带原因）
     */
    public static SystemException system(I18nExceptionCode exceptionCode, Throwable cause) {
        return new SystemException(exceptionCode, cause);
    }

    /**
     * 创建系统异常（带参数和原因）
     */
    public static SystemException system(I18nExceptionCode exceptionCode, Throwable cause, Object... args) {
        return new SystemException(exceptionCode, args, cause);
    }

    /**
     * 抛出系统异常
     */
    public static void throwSystem(I18nExceptionCode exceptionCode) {
        throw system(exceptionCode);
    }

    /**
     * 抛出系统异常（带参数）
     */
    public static void throwSystem(I18nExceptionCode exceptionCode, Object... args) {
        throw system(exceptionCode, args);
    }

    /**
     * 抛出系统异常（带原因）
     */
    public static void throwSystem(I18nExceptionCode exceptionCode, Throwable cause) {
        throw system(exceptionCode, cause);
    }

    /**
     * 条件抛出系统异常
     */
    public static void throwSystemIf(boolean condition, I18nExceptionCode exceptionCode) {
        if (condition) {
            throwSystem(exceptionCode);
        }
    }

    /**
     * 条件抛出系统异常（带参数）
     */
    public static void throwSystemIf(boolean condition, I18nExceptionCode exceptionCode, Object... args) {
        if (condition) {
            throwSystem(exceptionCode, args);
        }
    }

    // ==================== 便捷方法 ====================

    /**
     * 数据不存在异常
     */
    public static void throwNotFound() {
        throwBusiness(I18nCommonExceptionCode.DATA_NOT_FOUND);
    }

    /**
     * 数据不存在异常（带参数）
     */
    public static void throwNotFound(Object... args) {
        throwBusiness(I18nCommonExceptionCode.DATA_NOT_FOUND, args);
    }

    /**
     * 数据已存在异常
     */
    public static void throwAlreadyExists() {
        throwBusiness(I18nCommonExceptionCode.DATA_ALREADY_EXISTS);
    }

    /**
     * 数据已存在异常（带参数）
     */
    public static void throwAlreadyExists(Object... args) {
        throwBusiness(I18nCommonExceptionCode.DATA_ALREADY_EXISTS, args);
    }

    /**
     * 未授权异常
     */
    public static void throwUnauthorized() {
        throwBusiness(I18nCommonExceptionCode.UNAUTHORIZED);
    }

    /**
     * 未授权异常（带参数）
     */
    public static void throwUnauthorized(Object... args) {
        throwBusiness(I18nCommonExceptionCode.UNAUTHORIZED, args);
    }

    /**
     * 权限不足异常
     */
    public static void throwForbidden() {
        throwBusiness(I18nCommonExceptionCode.FORBIDDEN);
    }

    /**
     * 权限不足异常（带参数）
     */
    public static void throwForbidden(Object... args) {
        throwBusiness(I18nCommonExceptionCode.FORBIDDEN, args);
    }

    /**
     * 参数错误异常
     */
    public static void throwBadRequest() {
        throwBusiness(I18nCommonExceptionCode.BAD_REQUEST);
    }

    /**
     * 参数错误异常（带参数）
     */
    public static void throwBadRequest(Object... args) {
        throwBusiness(I18nCommonExceptionCode.BAD_REQUEST, args);
    }

    /**
     * 用户不存在异常
     */
    public static void throwUserNotFound() {
        throwBusiness(I18nCommonExceptionCode.USER_NOT_FOUND);
    }

    /**
     * 用户已存在异常
     */
    public static void throwUserAlreadyExists() {
        throwBusiness(I18nCommonExceptionCode.USER_ALREADY_EXISTS);
    }

    /**
     * 登录失败异常
     */
    public static void throwLoginFailed() {
        throwBusiness(I18nCommonExceptionCode.LOGIN_FAILED);
    }

    /**
     * Token过期异常
     */
    public static void throwTokenExpired() {
        throwBusiness(I18nCommonExceptionCode.TOKEN_EXPIRED);
    }

    /**
     * Token无效异常
     */
    public static void throwTokenInvalid() {
        throwBusiness(I18nCommonExceptionCode.TOKEN_INVALID);
    }

    /**
     * 数据库异常
     */
    public static void throwDatabaseError() {
        throwSystem(I18nCommonExceptionCode.DATABASE_ERROR);
    }

    /**
     * 数据库异常（带原因）
     */
    public static void throwDatabaseError(Throwable cause) {
        throwSystem(I18nCommonExceptionCode.DATABASE_ERROR, cause);
    }

    /**
     * 网络异常
     */
    public static void throwNetworkError() {
        throwSystem(I18nCommonExceptionCode.NETWORK_ERROR);
    }

    /**
     * 超时异常
     */
    public static void throwTimeout() {
        throwSystem(I18nCommonExceptionCode.TIMEOUT_ERROR);
    }
    
    // ==================== 安全相关异常 ====================
    
    /**
     * 认证失败异常
     */
    public static void throwAuthenticationFailed() {
        throwBusiness(I18nCommonExceptionCode.UNAUTHORIZED);
    }
    
    /**
     * 认证失败异常（带原因）
     */
    public static void throwAuthenticationFailed(String reason) {
        throwBusiness(I18nCommonExceptionCode.UNAUTHORIZED, reason);
    }
    
    /**
     * 权限不足异常
     */
    public static void throwPermissionDenied() {
        throwBusiness(I18nCommonExceptionCode.PERMISSION_DENIED);
    }
    
    /**
     * 权限不足异常（带资源信息）
     */
    public static void throwPermissionDenied(String resource) {
        throwBusiness(I18nCommonExceptionCode.PERMISSION_DENIED, resource);
    }
    
    /**
     * 账户锁定异常
     */
    public static void throwAccountLocked() {
        throwBusiness(I18nCommonExceptionCode.ACCOUNT_LOCKED);
    }
    
    /**
     * Token过期异常
     */
    public static void throwTokenExpired() {
        throwBusiness(I18nCommonExceptionCode.TOKEN_EXPIRED);
    }
    
    /**
     * Token无效异常
     */
    public static void throwTokenInvalid() {
        throwBusiness(I18nCommonExceptionCode.TOKEN_INVALID);
    }
    
    /**
     * 会话过期异常
     */
    public static void throwSessionExpired() {
        throwBusiness(I18nCommonExceptionCode.SESSION_EXPIRED);
    }
    
    /**
     * 条件认证异常
     * 
     * @param condition 条件
     * @param message 错误消息
     */
    public static void throwAuthenticationIf(boolean condition, String message) {
        if (condition) {
            throwAuthenticationFailed(message);
        }
    }
    
    /**
     * 条件权限异常
     * 
     * @param condition 条件
     * @param resource 资源
     */
    public static void throwPermissionIf(boolean condition, String resource) {
        if (condition) {
            throwPermissionDenied(resource);
        }
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