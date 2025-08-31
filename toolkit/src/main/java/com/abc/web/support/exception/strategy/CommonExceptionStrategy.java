package com.abc.web.support.exception.strategy;

import com.abc.web.support.ExceptionUtils;
import com.abc.web.support.enums.I18nCommonExceptionCode;
import com.abc.web.support.exception.ExceptionHandlerResult;
import com.abc.web.support.exception.ExceptionHandlerStrategy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;

/**
 * 通用异常处理策略
 * 处理常见的框架异常和系统异常
 * 
 * 作为兜底策略，处理其他策略未覆盖的异常类型
 * 
 * 处理场景：
 * - 数据库相关异常
 * - 网络IO异常  
 * - HTTP方法不支持
 * - 其他未分类的系统异常
 * 
 * 设计目标：
 * - 提供友好的错误提示
 * - 隐藏技术实现细节
 * - 便于运维监控
 * - 支持问题排查
 */
@Component
public class CommonExceptionStrategy implements ExceptionHandlerStrategy {

    @Override
    public boolean supports(Exception exception) {
        return exception instanceof DataAccessException
                || exception instanceof SQLException
                || exception instanceof IOException
                || exception instanceof HttpRequestMethodNotSupportedException
                || isGenericException(exception);
    }

    @Override
    public ExceptionHandlerResult handle(Exception exception) {
        if (exception instanceof DataAccessException || exception instanceof SQLException) {
            return handleDatabaseException(exception);
        } else if (exception instanceof SocketTimeoutException) {
            return handleTimeoutException(exception);
        } else if (exception instanceof IOException) {
            return handleIOException(exception);
        } else if (exception instanceof HttpRequestMethodNotSupportedException) {
            return handleMethodNotSupported((HttpRequestMethodNotSupportedException) exception);
        } else {
            return handleGenericException(exception);
        }
    }

    /**
     * 处理数据库相关异常
     */
    private ExceptionHandlerResult handleDatabaseException(Exception exception) {
        return ExceptionHandlerResult.system(I18nCommonExceptionCode.DATABASE_ERROR)
                .toBuilder()
                .details("Database operation failed: " + exception.getMessage())
                .build();
    }

    /**
     * 处理超时异常
     */
    private ExceptionHandlerResult handleTimeoutException(Exception exception) {
        return ExceptionHandlerResult.system(I18nCommonExceptionCode.TIMEOUT_ERROR)
                .toBuilder()
                .details("Request timeout: " + exception.getMessage())
                .build();
    }

    /**
     * 处理IO异常
     */
    private ExceptionHandlerResult handleIOException(Exception exception) {
        if (exception instanceof SocketTimeoutException) {
            return handleTimeoutException(exception);
        }
        
        return ExceptionHandlerResult.system(I18nCommonExceptionCode.EXTERNAL_SERVICE_ERROR)
                .toBuilder()
                .details("IO exception: " + exception.getMessage())
                .build();
    }

    /**
     * 处理HTTP方法不支持异常
     */
    private ExceptionHandlerResult handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        String supportedMethods = String.join(", ", exception.getSupportedMethods());
        String baseMessage = ExceptionUtils.getLocalizedMessage(I18nCommonExceptionCode.METHOD_NOT_ALLOWED);
        
        String message = String.format("%s %s，支持的方法: %s", 
                baseMessage, exception.getMethod(), supportedMethods);
        
        return ExceptionHandlerResult.validation(
                I18nCommonExceptionCode.METHOD_NOT_ALLOWED.getCode(),
                message
        ).toBuilder()
                .details("Unsupported HTTP method: " + exception.getMethod())
                .build();
    }

    /**
     * 处理通用异常（兜底处理）
     */
    private ExceptionHandlerResult handleGenericException(Exception exception) {
        // 对于未知异常，不向用户暴露详细信息
        return ExceptionHandlerResult.system(I18nCommonExceptionCode.INTERNAL_SERVER_ERROR)
                .toBuilder()
                .details("Unexpected exception: " + exception.getClass().getSimpleName() + 
                        " - " + exception.getMessage())
                .build();
    }

    /**
     * 判断是否为通用异常
     */
    private boolean isGenericException(Exception exception) {
        // 对于没有被其他策略处理的异常，都视为通用异常
        return !(exception instanceof RuntimeException && 
                 exception.getClass().getPackage().getName().startsWith("com.abc.web.support.exception"));
    }

    @Override
    public int getPriority() {
        // 通用策略优先级最低，作为兜底处理
        return Integer.MAX_VALUE;
    }
}