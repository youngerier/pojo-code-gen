package io.github.youngerier.support.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 异常处理策略管理器
 * 统一管理和调度所有异常处理策略
 * 
 * 架构设计：
 * 1. 策略模式：将不同类型异常的处理逻辑封装到不同策略中
 * 2. 责任链模式：按优先级顺序查找合适的处理策略
 * 3. 自动发现：通过Spring自动装配所有策略实现
 * 4. 优先级排序：支持策略优先级，灵活控制处理顺序
 * 
 * 使用方式：
 * ```java
 * @Component
 * public class CustomExceptionStrategy implements ExceptionHandlerStrategy {
 *     public boolean supports(Exception exception) { ... }
 *     public ExceptionHandlerResult handle(Exception exception) { ... }
 * }
 * ```
 */
@Slf4j
@Component
public class ExceptionStrategyManager {

    private final List<ExceptionHandlerStrategy> strategies;

    /**
     * 构造函数，自动注入所有异常处理策略
     * Spring会自动发现所有ExceptionHandlerStrategy的实现类
     */
    @Autowired
    public ExceptionStrategyManager(List<ExceptionHandlerStrategy> strategies) {
        this.strategies = strategies;
        // 按优先级排序，优先级数值越小越优先
        this.strategies.sort(Comparator.comparingInt(ExceptionHandlerStrategy::getPriority));
        
        if (log.isInfoEnabled()) {
            log.info("异常处理策略管理器初始化完成，共加载 {} 个策略：", strategies.size());
            strategies.forEach(strategy -> 
                log.info("  - {} (优先级: {})", 
                    strategy.getClass().getSimpleName(), 
                    strategy.getPriority())
            );
        }
    }

    /**
     * 处理异常
     * 按优先级顺序查找合适的策略进行处理
     *
     * @param exception 异常实例
     * @return 异常处理结果
     */
    public ExceptionHandlerResult handle(Exception exception) {
        if (exception == null) {
            log.warn("尝试处理空异常，返回默认错误响应");
            return createDefaultErrorResult();
        }

        // 查找支持处理该异常的策略
        Optional<ExceptionHandlerStrategy> strategy = strategies.stream()
                .filter(s -> {
                    try {
                        return s.supports(exception);
                    } catch (Exception e) {
                        log.warn("策略 {} 判断异常支持时发生错误: {}", 
                                s.getClass().getSimpleName(), e.getMessage());
                        return false;
                    }
                })
                .findFirst();

        if (strategy.isPresent()) {
            try {
                ExceptionHandlerResult result = strategy.get().handle(exception);
                if (log.isDebugEnabled()) {
                    log.debug("异常 {} 被策略 {} 处理", 
                            exception.getClass().getSimpleName(), 
                            strategy.get().getClass().getSimpleName());
                }
                return result;
            } catch (Exception e) {
                log.error("策略 {} 处理异常时发生错误: {}", 
                        strategy.get().getClass().getSimpleName(), e.getMessage(), e);
                return createDefaultErrorResult();
            }
        }

        // 没有找到合适的策略，使用默认处理
        log.warn("未找到处理异常 {} 的策略，使用默认处理", exception.getClass().getSimpleName());
        return createDefaultErrorResult();
    }

    /**
     * 创建默认错误响应
     */
    private ExceptionHandlerResult createDefaultErrorResult() {
        return ExceptionHandlerResult.system("500", "系统内部错误");
    }

    /**
     * 获取已注册的策略数量
     */
    public int getStrategyCount() {
        return strategies.size();
    }

    /**
     * 获取所有策略的类名列表（用于调试）
     */
    public List<String> getStrategyNames() {
        return strategies.stream()
                .map(strategy -> strategy.getClass().getSimpleName())
                .toList();
    }
}