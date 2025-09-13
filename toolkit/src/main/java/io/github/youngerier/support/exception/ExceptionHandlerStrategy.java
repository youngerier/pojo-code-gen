package io.github.youngerier.support.exception;

/**
 * 异常处理策略接口
 * 定义统一的异常处理契约，支持不同类型异常的个性化处理
 * 
 * 设计理念：
 * 1. 统一性：所有异常处理都通过策略模式统一管理
 * 2. 可扩展性：新的异常类型可以轻松添加处理策略
 * 3. 职责分离：每个策略专注处理特定类型的异常
 * 4. 开发友好：简单清晰的接口，易于理解和实现
 */
public interface ExceptionHandlerStrategy {

    /**
     * 判断是否支持处理该异常
     *
     * @param exception 异常实例
     * @return true 如果支持处理
     */
    boolean supports(Exception exception);

    /**
     * 处理异常并返回统一的错误响应
     *
     * @param exception 异常实例
     * @return 错误响应结果
     */
    ExceptionHandlerResult handle(Exception exception);

    /**
     * 获取策略优先级
     * 数值越小优先级越高
     *
     * @return 优先级数值
     */
    default int getPriority() {
        return 0;
    }
}