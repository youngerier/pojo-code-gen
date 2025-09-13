package io.github.youngerier.support.exception;

/**
 * 业务异常
 * 用于所有业务逻辑处理中的异常情况，包括：
 * - 参数验证失败
 * - 业务规则违反
 * - 认证授权失败
 * - 数据不存在等
 */
public class BusinessException extends BaseException {

    public BusinessException(ExceptionCode exceptionCode) {
        super(exceptionCode);
    }

    public BusinessException(ExceptionCode exceptionCode, Object[] args) {
        super(exceptionCode, args);
    }

    public BusinessException(ExceptionCode exceptionCode, Throwable cause) {
        super(exceptionCode, cause);
    }

    public BusinessException(ExceptionCode exceptionCode, Object[] args, Throwable cause) {
        super(exceptionCode, args, cause);
    }
}