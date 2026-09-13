package io.github.youngerier.support.exception;

import io.github.youngerier.support.message.MessageFormatter;
import io.github.youngerier.support.message.MessagePlaceholder;
import lombok.Getter;

import java.io.Serial;

/**
 * 通用业务异常，支持两种消息模式：
 *
 * <p><b>1. 字面量消息</b>（快速使用，不做国际化）：
 * <pre>{@code
 * throw BaseException.notFound("用户不存在");
 * throw BaseException.notFound("用户 {} 不存在", id);   // slf4j 风格 {} 占位，服务端立即拼接
 * }</pre>
 *
 * <p><b>2. 国际化消息</b>（推荐用于对外系统）：异常只携带异常码与参数，
 * 由全局异常处理器在 Web 边缘按请求 Locale 解析消息：
 * <pre>{@code
 * // 枚举实现 ExceptionCode（getMessageKey() 对应 messages 中的 key）
 * throw BaseException.i18n(UserErrorCode.USER_NOT_FOUND, id);
 * }</pre>
 *
 * <p>不希望向调用方暴露内部细节时使用 {@link #friendly(String)}，
 * 响应统一返回异常码对应的通用提示，真实信息只记日志。
 */
@Getter
public class BaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final MessageFormatter MESSAGE_FORMATTER = MessageFormatter.slf4j();

    private final ExceptionCode code;

    private final ExceptionLogLevel logLevel;

    /**
     * 是否对调用方隐藏真实消息，仅返回异常码对应的通用文案
     */
    private final boolean friendly;

    /**
     * 是否为国际化消息：为 true 时 {@link #getMessage()} 是消息键，
     * 消息参数见 {@link #messageArgs}，由边缘处理器解析
     */
    private final boolean i18n;

    /**
     * 国际化消息参数（MessageFormat 风格 {0}/{1}）
     */
    private final Object[] messageArgs;

    public BaseException(String message) {
        this(DefaultExceptionCode.INTERNAL_SERVER_ERROR, message);
    }

    public BaseException(ExceptionCode code, String message) {
        this(code, defaultLogLevel(code), message, null, false, false, null);
    }

    public BaseException(ExceptionCode code, String message, Throwable cause) {
        this(code, defaultLogLevel(code), message, cause, false, false, null);
    }

    public BaseException(ExceptionCode code, ExceptionLogLevel logLevel, String message) {
        this(code, logLevel, message, null, false, false, null);
    }

    public BaseException(ExceptionCode code, ExceptionLogLevel logLevel, String message, Throwable cause) {
        this(code, logLevel, message, cause, false, false, null);
    }

    private BaseException(ExceptionCode code, ExceptionLogLevel logLevel, String message,
                          Throwable cause, boolean friendly, boolean i18n, Object[] messageArgs) {
        super(message == null ? code.getDesc() : message, cause);
        this.code = code;
        this.logLevel = logLevel;
        this.friendly = friendly;
        this.i18n = i18n;
        this.messageArgs = messageArgs;
    }

    /**
     * 5xx 默认 ERROR，其余（含 4xx 与业务码）默认 WARN
     */
    private static ExceptionLogLevel defaultLogLevel(ExceptionCode code) {
        return code.httpStatus() >= 500 ? ExceptionLogLevel.ERROR : ExceptionLogLevel.WARN;
    }

    // ---------------- 国际化消息 ----------------

    /**
     * 构造国际化异常：消息在边缘按 {@link ExceptionCode#getMessageKey()} 与请求 Locale 解析，
     * 参数使用 MessageFormat 风格（{0}、{1}）。
     *
     * @param code        异常码（需提供 messageKey）
     * @param messageArgs 消息参数
     */
    public static BaseException i18n(ExceptionCode code, Object... messageArgs) {
        return new BaseException(code, defaultLogLevel(code), code.getMessageKey(),
                null, false, true, messageArgs);
    }

    // ---------------- 字面量消息工厂 ----------------

    /**
     * 基于消息占位符构造（slf4j 风格 {} 语法）
     */
    public static BaseException of(ExceptionCode code, String pattern, Object... args) {
        return new BaseException(code, format(pattern, args));
    }

    /**
     * 基于消息占位符构造通用异常
     */
    public static BaseException common(String pattern, Object... args) {
        return new BaseException(format(pattern, args));
    }

    public static BaseException common(MessagePlaceholder placeholder) {
        return new BaseException(format(placeholder.pattern(), placeholder.args()));
    }

    public static BaseException badRequest(String pattern, Object... args) {
        return new BaseException(DefaultExceptionCode.BAD_REQUEST, format(pattern, args));
    }

    public static BaseException unauthorized(String pattern, Object... args) {
        return new BaseException(DefaultExceptionCode.UNAUTHORIZED, format(pattern, args));
    }

    public static BaseException forbidden(String pattern, Object... args) {
        return new BaseException(DefaultExceptionCode.FORBIDDEN, format(pattern, args));
    }

    public static BaseException forbidden(ExceptionLogLevel level, String message) {
        return new BaseException(DefaultExceptionCode.FORBIDDEN, level, message);
    }

    public static BaseException notFound(String pattern, Object... args) {
        return new BaseException(DefaultExceptionCode.NOT_FOUND, format(pattern, args));
    }

    public static BaseException conflict(String pattern, Object... args) {
        return new BaseException(DefaultExceptionCode.CONFLICT, format(pattern, args));
    }

    public static BaseException tooManyRequests(String pattern, Object... args) {
        return new BaseException(DefaultExceptionCode.TOO_MANY_REQUESTS, format(pattern, args));
    }

    public static BaseException serviceUnavailable(String pattern, Object... args) {
        return new BaseException(DefaultExceptionCode.SERVICE_UNAVAILABLE, format(pattern, args));
    }

    /**
     * 使用自定义业务码构造字面量异常
     */
    public static BaseException business(ExceptionCode code, String pattern, Object... args) {
        return new BaseException(code, format(pattern, args));
    }

    /**
     * 友好异常：真实信息只进日志，响应统一返回通用提示，避免内部细节泄漏给调用方
     */
    public static BaseException friendly(String message) {
        return friendly(DefaultExceptionCode.INTERNAL_SERVER_ERROR, message);
    }

    public static BaseException friendly(ExceptionCode code, String message) {
        return new BaseException(code, ExceptionLogLevel.WARN, message, null, true, false, null);
    }

    public String getTextCode() {
        return code.getCode();
    }

    private static String format(String pattern, Object... args) {
        if (pattern == null) {
            return null;
        }
        return args == null || args.length == 0 ? pattern : MESSAGE_FORMATTER.format(pattern, args);
    }
}
