package io.github.youngerier.support;

import io.github.youngerier.support.exception.BaseException;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 业务断言工具，断言失败抛出 {@link BaseException}。
 * 消息支持 slf4j 风格的 {@code {}} 占位符。
 */
public final class AssertUtils {

    private AssertUtils() {
        throw new AssertionError();
    }

    public static void state(boolean expression, Supplier<RuntimeException> exceptionSupplier) {
        if (!expression) {
            throw Objects.requireNonNull(exceptionSupplier.get(), "exception must not be null");
        }
    }

    /**
     * 参数断言：失败抛 400（{@link BaseException#badRequest}）。
     * 用于校验调用方入参，区别于服务内部状态错误（500）。
     */
    public static void isTrue(boolean expression, String message, Object... args) {
        if (!expression) {
            throw BaseException.badRequest(message, args);
        }
    }

    public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw BaseException.badRequest(nullSafeGet(messageSupplier));
        }
    }

    public static void isFalse(boolean expression, String message, Object... args) {
        isTrue(!expression, message, args);
    }

    public static void isNull(@Nullable Object object, String message, Object... args) {
        isTrue(object == null, message, args);
    }

    public static void notNull(@Nullable Object object, String message, Object... args) {
        isTrue(object != null, message, args);
    }

    public static void hasText(@Nullable String text, String message, Object... args) {
        isTrue(StringUtils.hasText(text), message, args);
    }

    public static void notEmpty(@Nullable Collection<?> collection, String message, Object... args) {
        isTrue(!CollectionUtils.isEmpty(collection), message, args);
    }

    public static void notEmpty(@Nullable Map<?, ?> map, String message, Object... args) {
        isTrue(!CollectionUtils.isEmpty(map), message, args);
    }

    public static void notEmpty(@Nullable Object[] array, String message, Object... args) {
        isTrue(array != null && array.length > 0, message, args);
    }

    private static String nullSafeGet(Supplier<String> messageSupplier) {
        return messageSupplier == null ? null : messageSupplier.get();
    }
}
