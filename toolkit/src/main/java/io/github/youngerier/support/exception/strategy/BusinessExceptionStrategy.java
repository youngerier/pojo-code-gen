package io.github.youngerier.support.exception.strategy;

import io.github.youngerier.support.ExceptionUtils;
import io.github.youngerier.support.exception.BusinessException;
import io.github.youngerier.support.exception.ExceptionHandlerResult;
import io.github.youngerier.support.exception.ExceptionHandlerStrategy;
import io.github.youngerier.support.exception.I18nExceptionCode;
import org.springframework.stereotype.Component;

/**
 * 业务异常处理策略
 * 处理所有业务逻辑相关的异常
 * 
 * 处理原则：
 * 1. 业务异常通常是用户操作导致的，属于可预期的异常
 * 2. 返回HTTP 200状态码，但在业务层面标识错误
 * 3. 提供用户友好的错误消息
 * 4. 记录WARN级别日志，不需要ERROR级别
 * 
 * 适用场景：
 * - 参数验证失败
 * - 业务规则违反
 * - 数据状态异常
 * - 权限不足等
 */
@Component
public class BusinessExceptionStrategy implements ExceptionHandlerStrategy {

    @Override
    public boolean supports(Exception exception) {
        return exception instanceof BusinessException;
    }

    @Override
    public ExceptionHandlerResult handle(Exception exception) {
        BusinessException businessException = (BusinessException) exception;
        
        // 检查是否为I18nExceptionCode，如果是则使用简化API
        if (businessException.getExceptionCode() instanceof I18nExceptionCode i18nCode) {
            if (businessException.getArgs() != null && businessException.getArgs().length > 0) {
                return ExceptionHandlerResult.business(i18nCode, businessException.getArgs());
            } else {
                return ExceptionHandlerResult.business(i18nCode);
            }
        } else {
            // 兜底处理，使用原有方式
            String message = ExceptionUtils.getLocalizedMessage(businessException);
            return ExceptionHandlerResult.business(
                    businessException.getExceptionCode().getCode(),
                    message
            );
        }
    }

    @Override
    public int getPriority() {
        // 业务异常优先级较高，优先处理
        return 10;
    }
}