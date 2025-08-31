package com.abc.web.support.exception.strategy;

import com.abc.web.support.ExceptionUtils;
import com.abc.web.support.enums.I18nCommonExceptionCode;
import com.abc.web.support.exception.ExceptionHandlerResult;
import com.abc.web.support.exception.ExceptionHandlerStrategy;
import org.springframework.stereotype.Component;

/**
 * 安全异常处理策略
 * 处理安全相关的认证授权异常
 * 
 * 处理原则：
 * 1. 认证异常返回401，提示用户重新登录
 * 2. 授权异常返回403，提示权限不足
 * 3. 账户状态异常提供明确的错误提示
 * 4. 安全异常不暴露敏感信息
 * 
 * 企业安全要求：
 * - 统一的安全异常处理
 * - 防止信息泄露
 * - 便于安全审计
 * - 用户友好的错误提示
 * 
 * 注意：这个策略不依赖Spring Security，可以处理任何安全相关异常
 */
@Component
public class SecurityExceptionStrategy implements ExceptionHandlerStrategy {

    @Override
    public boolean supports(Exception exception) {
        String exceptionName = exception.getClass().getSimpleName();
        
        // 支持处理安全相关的异常（通过类名判断，避免直接依赖）
        return exceptionName.contains("Authentication") ||
               exceptionName.contains("AccessDenied") ||
               exceptionName.contains("BadCredentials") ||
               exceptionName.contains("AccountLocked") ||
               exceptionName.contains("AccountDisabled") ||
               exceptionName.contains("Locked") ||
               exceptionName.contains("Disabled");
    }

    @Override
    public ExceptionHandlerResult handle(Exception exception) {
        String exceptionName = exception.getClass().getSimpleName();
        
        if (exceptionName.contains("BadCredentials")) {
            return handleBadCredentials(exception);
        } else if (exceptionName.contains("Locked")) {
            return handleAccountLocked(exception);
        } else if (exceptionName.contains("Disabled")) {
            return handleAccountDisabled(exception);
        } else if (exceptionName.contains("AccessDenied")) {
            return handleAccessDenied(exception);
        } else if (exceptionName.contains("Authentication")) {
            return handleAuthenticationException(exception);
        }
        
        // 默认认证失败处理
        return ExceptionHandlerResult.authentication(I18nCommonExceptionCode.LOGIN_FAILED);
    }

    /**
     * 处理凭据错误异常（用户名密码错误）
     */
    private ExceptionHandlerResult handleBadCredentials(Exception exception) {
        return ExceptionHandlerResult.authentication(I18nCommonExceptionCode.LOGIN_FAILED);
    }

    /**
     * 处理账户锁定异常
     */
    private ExceptionHandlerResult handleAccountLocked(Exception exception) {
        return ExceptionHandlerResult.authentication(I18nCommonExceptionCode.ACCOUNT_LOCKED);
    }

    /**
     * 处理账户禁用异常
     */
    private ExceptionHandlerResult handleAccountDisabled(Exception exception) {
        return ExceptionHandlerResult.authentication(I18nCommonExceptionCode.USER_DISABLED);
    }

    /**
     * 处理访问拒绝异常（权限不足）
     */
    private ExceptionHandlerResult handleAccessDenied(Exception exception) {
        return ExceptionHandlerResult.authorization(I18nCommonExceptionCode.PERMISSION_DENIED);
    }

    /**
     * 处理通用认证异常
     */
    private ExceptionHandlerResult handleAuthenticationException(Exception exception) {
        return ExceptionHandlerResult.authentication(I18nCommonExceptionCode.UNAUTHORIZED);
    }

    @Override
    public int getPriority() {
        // 安全异常优先级较高，仅次于验证异常
        return 15;
    }
}