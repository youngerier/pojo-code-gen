package io.github.youngerier.support.exception;

import java.util.Locale;

/**
 * 异常码。code 既作为响应体中的错误码，也用于推导 HTTP 状态码：
 * <ul>
 *     <li>4xx / 5xx 数字码：直接作为 HTTP 状态码</li>
 *     <li>其他数字码（业务码，如 10001）：HTTP 422（业务规则校验失败）</li>
 *     <li>非数字码：HTTP 500</li>
 * </ul>
 *
 * <p><b>多语言开箱即用</b>：只要以枚举形式实现本接口，消息键自动按约定
 * {@code exception.} + 枚举名小写生成（如 {@code USER_NOT_FOUND} → {@code exception.user_not_found}），
 * 无需覆写任何方法。消息可来自应用的 {@code messages.properties}、数据库（实现
 * {@code ExceptionMessageProvider}），或 toolkit 内置的中英文 bundle；均未配置时回退 {@link #getDesc()}。
 * 非枚举实现可覆写 {@link #getMessageKey()} 显式指定消息键。
 */
public interface ExceptionCode {

    /**
     * @return 异常码
     */
    String getCode();

    /**
     * @return 异常描述（无国际化消息时的兜底文案）
     */
    String getDesc();

    /**
     * 国际化消息键。枚举实现默认按约定 {@code exception.} + 枚举名小写；
     * 非枚举实现默认 null（不解析，直接使用 desc），可覆写指定。
     */
    default String getMessageKey() {
        if (this instanceof Enum<?> enumValue) {
            return "exception." + enumValue.name().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    /**
     * 由异常码推导 HTTP 状态码
     */
    default int httpStatus() {
        try {
            int code = Integer.parseInt(getCode());
            if (code >= 400 && code < 600) {
                return code;
            }
            // 非 HTTP 语义的业务码：请求格式正确但不满足业务规则
            return 422;
        } catch (NumberFormatException e) {
            return 500;
        }
    }
}
