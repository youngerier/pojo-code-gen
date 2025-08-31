package com.abc.web.support.exception.strategy;

import com.abc.web.support.ExceptionUtils;
import com.abc.web.support.exception.ExceptionHandlerResult;
import com.abc.web.support.exception.ExceptionHandlerStrategy;
import com.abc.web.support.exception.I18nExceptionCode;
import com.abc.web.support.exception.SystemException;
import org.springframework.stereotype.Component;

/**
 * 系统异常处理策略
 * 处理系统级别的异常，如数据库异常、网络异常等
 * 
 * 处理原则：
 * 1. 系统异常通常是不可预期的，需要技术人员介入
 * 2. 返回HTTP 500状态码，标识服务器内部错误
 * 3. 对用户隐藏技术细节，提供通用错误消息
 * 4. 记录ERROR级别日志，便于问题排查
 * 
 * 适用场景：
 * - 数据库连接异常
 * - 外部服务调用失败
 * - 文件IO异常
 * - 网络超时等
 */
@Component
public class SystemExceptionStrategy implements ExceptionHandlerStrategy {

    @Override
    public boolean supports(Exception exception) {
        return exception instanceof SystemException;
    }

    @Override
    public ExceptionHandlerResult handle(Exception exception) {
        SystemException systemException = (SystemException) exception;
        
        // 检查是否为I18nExceptionCode，如果是则使用简化API
        if (systemException.getExceptionCode() instanceof I18nExceptionCode i18nCode) {
            if (systemException.getArgs() != null && systemException.getArgs().length > 0) {
                return ExceptionHandlerResult.system(i18nCode, systemException.getArgs());
            } else {
                return ExceptionHandlerResult.system(i18nCode);
            }
        } else {
            // 兜底处理，使用原有方式
            String message = ExceptionUtils.getLocalizedMessage(systemException);
            return ExceptionHandlerResult.system(
                    systemException.getExceptionCode().getCode(),
                    message
            );
        }
    }

    @Override
    public int getPriority() {
        // 系统异常优先级较高
        return 20;
    }
}