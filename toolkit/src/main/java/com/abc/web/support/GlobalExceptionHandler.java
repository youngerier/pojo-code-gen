package com.abc.web.support;

import com.abc.web.support.exception.ExceptionHandlerResult;
import com.abc.web.support.exception.ExceptionStrategyManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * 全局异常处理器
 * 
 * 架构设计：
 * 1. 统一入口：所有异常都通过这个处理器统一处理
 * 2. 策略模式：具体处理逻辑委托给ExceptionStrategyManager
 * 3. 链路追踪：为每个异常生成traceId，便于问题排查
 * 4. 监控友好：统一的日志格式和监控埋点
 * 
 * 开发体验：
 * - 开发者只需要关注业务逻辑，异常处理自动化
 * - 统一的错误响应格式，前端处理简单
 * - 丰富的错误信息，便于调试和问题定位
 * - 支持自定义异常策略，扩展简单
 * 
 * 使用方式：
 * 1. 抛出异常：直接抛出BusinessException、SystemException等
 * 2. 自定义策略：实现ExceptionHandlerStrategy接口
 * 3. 监控集成：通过traceId关联日志和监控系统
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ExceptionStrategyManager strategyManager;

    @Autowired
    public GlobalExceptionHandler(ExceptionStrategyManager strategyManager) {
        this.strategyManager = strategyManager;
        log.info("全局异常处理器初始化完成，已加载 {} 个异常处理策略", strategyManager.getStrategyCount());
    }

    /**
     * 统一异常处理入口
     * 所有未被特定Handler捕获的异常都会进入这里
     * 
     * @param exception 异常实例
     * @param request   HTTP请求
     * @return 统一的错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleException(Exception exception, HttpServletRequest request) {
        // 生成追踪ID
        String traceId = generateTraceId();
        
        // 获取请求路径
        String requestPath = getRequestPath(request);
        
        // 委托给策略管理器处理
        ExceptionHandlerResult result = strategyManager.handle(exception);
        
        // 设置请求相关信息
        result.setTraceId(traceId);
        result.setPath(requestPath);
        
        // 记录日志
        logException(exception, result, traceId, requestPath);
        
        // 构建响应
        Response<Void> response = Response.error(
                Integer.parseInt(result.getErrorCode()),
                result.getMessage()
        );
        
        // 在开发环境下可以添加更多调试信息
        if (log.isDebugEnabled() && result.getDetails() != null) {
            log.debug("异常详细信息: {}", result.getDetails());
        }
        
        return ResponseEntity
                .status(result.getHttpStatus())
                .header("X-Trace-Id", traceId)
                .body(response);
    }

    /**
     * 记录异常日志
     * 根据异常级别记录不同级别的日志
     */
    private void logException(Exception exception, ExceptionHandlerResult result, String traceId, String path) {
        if (!result.isShouldLog()) {
            return;
        }

        String logMessage = "异常处理 [{}] - 路径: {}, 错误码: {}, 消息: {}, 异常: {}";
        Object[] logArgs = {traceId, path, result.getErrorCode(), result.getMessage(), exception.getClass().getSimpleName()};

        switch (result.getLogLevel()) {
            case DEBUG -> {
                if (log.isDebugEnabled()) {
                    log.debug(logMessage, logArgs, exception);
                }
            }
            case INFO -> {
                if (log.isInfoEnabled()) {
                    log.info(logMessage, logArgs);
                }
            }
            case WARN -> {
                log.warn(logMessage, logArgs);
            }
            case ERROR -> {
                log.error(logMessage, logArgs, exception);
            }
        }
    }

    /**
     * 生成追踪ID
     * 用于关联请求、响应和日志
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 获取请求路径
     */
    private String getRequestPath(HttpServletRequest request) {
        if (request != null) {
            return request.getRequestURI();
        }
        
        // 尝试从RequestContextHolder获取
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest().getRequestURI();
        } catch (Exception e) {
            return "unknown";
        }
    }
}