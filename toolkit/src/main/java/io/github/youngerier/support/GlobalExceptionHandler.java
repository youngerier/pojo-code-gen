package io.github.youngerier.support;

import io.github.youngerier.support.exception.ExceptionHandlerResult;
import io.github.youngerier.support.exception.ExceptionStrategyManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * 企业级全局异常处理器
 * 
 * 设计原则：
 * 1. 类型安全：保持Response<Void>返回类型，符合Java最佳实践
 * 2. 环境敏感：根据运行环境控制日志详细程度，保护生产环境安全
 * 3. 单一职责：专注异常处理，不引入复杂依赖
 * 4. 配置化：通过配置控制行为，灵活且安全
 * 
 * 安全策略：
 * - 生产环境：隐藏堆栈信息，只记录关键错误信息
 * - 非生产环境：完整日志记录，便于调试
 * 
 * 技术专家级实现：
 * - 遵循Spring Boot最佳实践
 * - 保持API契约不变
 * - 性能优化，最小开销
 * - 企业级安全考虑
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ExceptionStrategyManager strategyManager;
    
    /**
     * 当前环境配置（通过Spring Profile自动注入）
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    /**
     * 是否在日志中显示堆栈信息（可配置，默认根据环境判断）
     */
    @Value("${logging.exception.stacktrace:#{null}}")
    private Boolean showStackTrace;

    @Autowired
    public GlobalExceptionHandler(ExceptionStrategyManager strategyManager) {
        this.strategyManager = strategyManager;
        log.info("全局异常处理器初始化完成，已加载 {} 个异常处理策略，当前环境: {}", 
                strategyManager.getStrategyCount(), activeProfile);
        
        if (isProductionEnvironment()) {
            log.warn("⚠️  生产环境已启用安全模式，异常详情和堆栈信息将被限制");
        }
    }

    /**
     * 统一异常处理入口
     * 
     * 企业级处理流程：
     * 1. 生成追踪ID
     * 2. 委托策略管理器处理异常
     * 3. 根据环境安全地记录日志（内部处理，保持API契约）
     * 4. 构建类型安全的Response响应
     * 
     * @param exception 异常实例
     * @param request   HTTP请求
     * @return 统一的错误响应（保持原有Response<Void>类型）
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
        
        // 环境敏感的日志记录（内部处理，不影响响应）
        logExceptionSecurely(exception, result, traceId, requestPath);
        
        // 构建类型安全的响应 - 保持原有Response<Void>类型
        Response<Void> response = Response.error(
                Integer.parseInt(result.getErrorCode()),
                result.getMessage()
        );
        
        return ResponseEntity
                .status(result.getHttpStatus())
                .header("X-Trace-Id", traceId)
                .header("X-Environment", getEnvironmentType())
                .body(response);
    }

    /**
     * 环境敏感的日志记录
     * 根据环境和配置决定日志详细程度
     */
    private void logExceptionSecurely(Exception exception, ExceptionHandlerResult result, String traceId, String path) {
        if (!result.isShouldLog()) {
            return;
        }

        // 构建基础日志参数
        Object[] logArgs = buildLogArgs(traceId, path, result, exception);
        String logMessage = "异常处理 [{}] - 环境: {}, 路径: {}, 错误码: {}, 消息: {}, 异常类型: {}";
        
        // 根据日志级别记录
        logByLevel(result.getLogLevel(), logMessage, logArgs, exception);
        
        // 记录额外信息
        logAdditionalInfo(result, traceId, exception);
    }
    
    /**
     * 构建日志参数数组
     */
    private Object[] buildLogArgs(String traceId, String path, ExceptionHandlerResult result, Exception exception) {
        return new Object[]{
            traceId,
            getEnvironmentType(),
            path,
            result.getErrorCode(),
            result.getMessage(),
            exception.getClass().getSimpleName()
        };
    }
    
    /**
     * 根据日志级别记录日志
     */
    private void logByLevel(ExceptionHandlerResult.LogLevel level, String message, Object[] args, Exception exception) {
        switch (level) {
            case DEBUG -> {
                if (log.isDebugEnabled()) {
                    logWithOptionalException(log::debug, message, args, exception);
                }
            }
            case INFO -> log.info(message, args);
            case WARN -> log.warn(message, args);
            case ERROR -> logWithOptionalException(log::error, message, args, exception);
        }
    }
    
    /**
     * 根据配置决定是否包含异常堆栈的日志记录
     */
    private void logWithOptionalException(LogMethod logMethod, String message, Object[] args, Exception exception) {
        if (shouldShowStackTrace()) {
            // 创建新数组，添加异常作为最后一个参数
            Object[] argsWithException = new Object[args.length + 1];
            System.arraycopy(args, 0, argsWithException, 0, args.length);
            argsWithException[args.length] = exception;
            logMethod.log(message, argsWithException);
        } else {
            logMethod.log(message, args);
        }
    }
    
    /**
     * 记录额外信息（详细信息和安全审计）
     */
    private void logAdditionalInfo(ExceptionHandlerResult result, String traceId, Exception exception) {
        // 非生产环境记录详细信息
        if (!isProductionEnvironment() && result.getDetails() != null && 
            (result.getLogLevel() == ExceptionHandlerResult.LogLevel.WARN || 
             result.getLogLevel() == ExceptionHandlerResult.LogLevel.ERROR)) {
            log.warn("异常详细信息 [{}]: {}", traceId, sanitizeDetails(result.getDetails()));
        }
        
        // 生产环境安全审计
        if (isProductionEnvironment() && result.getLogLevel() == ExceptionHandlerResult.LogLevel.ERROR) {
            logSecurityAudit(traceId, result.getErrorCode(), exception);
        }
    }
    
    /**
     * 日志方法接口，用于统一不同级别的日志调用
     */
    @FunctionalInterface
    private interface LogMethod {
        void log(String message, Object... args);
    }

    /**
     * 生产环境安全审计日志
     */
    private void logSecurityAudit(String traceId, String errorCode, Exception exception) {
        // 记录安全审计信息，不包含敏感细节
        log.info("SECURITY_AUDIT [{}] - ErrorCode: {}, ExceptionType: {}", 
                traceId, errorCode, exception.getClass().getSimpleName());
    }

    /**
     * 清理敏感信息
     */
    private String sanitizeDetails(String details) {
        if (details == null) {
            return null;
        }
        
        String sanitized = details
                // 移除SQL语句中的敏感信息
                .replaceAll("(?i)(password|token|secret)\\s*=\\s*'[^']*'", "$1='***'")
                .replaceAll("(?i)(password|token|secret)\\s*=\\s*\"[^\"]*\"", "$1=\"***\"")
                // 移除IP地址
                .replaceAll("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b", "[IP]");
                
        // 限制长度
        return sanitized.substring(0, Math.min(sanitized.length(), 1000));
    }

    /**
     * 判断是否为生产环境
     */
    private boolean isProductionEnvironment() {
        return activeProfile != null && (
                activeProfile.toLowerCase().contains("prod") ||
                activeProfile.toLowerCase().contains("production") ||
                activeProfile.toLowerCase().contains("prd")
        );
    }

    /**
     * 获取环境类型
     */
    private String getEnvironmentType() {
        if (isProductionEnvironment()) {
            return "PRODUCTION";
        } else if (activeProfile != null && (
                activeProfile.toLowerCase().contains("test") ||
                activeProfile.toLowerCase().contains("sit") ||
                activeProfile.toLowerCase().contains("uat"))) {
            return "TEST";
        } else {
            return "DEVELOPMENT";
        }
    }

    /**
     * 判断是否应该显示堆栈跟踪
     */
    private boolean shouldShowStackTrace() {
        if (showStackTrace != null) {
            return showStackTrace;
        }
        
        // 默认策略：只有非生产环境才显示堆栈跟踪
        return !isProductionEnvironment();
    }

    /**
     * 生成追踪ID
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
        
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest().getRequestURI();
        } catch (Exception e) {
            return "unknown";
        }
    }
}