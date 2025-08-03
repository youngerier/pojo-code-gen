package com.abc.entity.web.support;


import com.abc.entity.web.exception.BaseException;
import com.abc.entity.web.support.message.MessagePlaceholder;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;


/**
 * 断言工具类，用于参数校验和前置条件检查
 * <p>
 * 该类提供了一系列静态方法，用于在代码中进行各种断言检查，当断言条件不满足时，
 * 会抛出统一的{@link BaseException}异常，便于上层统一处理。
 *
 * <p>主要特性：
 * <ul>
 *   <li>支持多种数据类型的校验：布尔值、对象、字符串、集合、数组、Map等</li>
 *   <li>支持国际化消息：通过{@link MessagePlaceholder}支持消息模板和参数</li>
 *   <li>支持Lambda表达式：提供基于{@link Supplier}的延迟消息构造，提高性能</li>
 *   <li>提供丰富的断言方法：类型检查、范围检查、正则表达式匹配等</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * // 检查参数非空
 * AssertUtils.notNull(userId, "用户ID不能为空");
 *
 * // 使用占位符
 * AssertUtils.isTrue(age >= 18, "用户[{0}]年龄必须大于等于18岁", username);
 *
 * // 检查集合非空
 * AssertUtils.notEmpty(userList, "用户列表不能为空");
 *
 * // 检查字符串格式
 * AssertUtils.matchesPattern(email, EMAIL_PATTERN, "邮箱格式不正确");
 *
 * // 检查数值范围
 * AssertUtils.isInRange(score, 0, 100, "分数必须在0-100之间");
 * </pre>
 *
 * @see BaseException 统一的异常类型
 * @see MessagePlaceholder 消息占位符支持
 */
public final class AssertUtils {
    private AssertUtils() {
        throw new AssertionError();
    }

    public static void state(boolean expression, Supplier<BaseException> exceptionSupplier) {
        if (!expression) {
            throw Objects.requireNonNull(nullSafeGet(exceptionSupplier), "exception must not null");
        }
    }

    public static void isTrue(boolean expression, MessagePlaceholder placeholder) {
        isTrue(expression, placeholder.getPattern(), placeholder.getArgs());
    }

    public static void isTrue(boolean expression, String message, Object... args) {
        state(expression, () -> BaseException.common(MessagePlaceholder.of(message, args)));
    }

    public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
        state(expression, () -> BaseException.common(nullSafeGet(messageSupplier)));
    }

    public static void isFalse(boolean expression, MessagePlaceholder placeholder) {
        isFalse(expression, placeholder.getPattern(), placeholder.getArgs());
    }

    public static void isFalse(boolean expression, String message, Object... args) {
        isTrue(!expression, message, args);
    }

    public static void isFalse(boolean expression, Supplier<String> messageSupplier) {
        isTrue(!expression, messageSupplier);
    }

    public static void isNull(@Nullable Object object, String message, Object... args) {
        isTrue(object == null, message, args);
    }

    public static void isNull(@Nullable Object object, Supplier<String> messageSupplier) {
        isTrue(object == null, messageSupplier);
    }

    public static void notNull(Object object, MessagePlaceholder placeholder) {
        notNull(object, placeholder.getPattern(), placeholder.getArgs());
    }

    public static void notNull(@Nullable Object object, String message, Object... args) {
        isTrue(object != null, message, args);
    }

    public static void notNull(@Nullable Object object, Supplier<String> messageSupplier) {
        isTrue(object != null, messageSupplier);
    }

    public static void hasLength(String text, MessagePlaceholder placeholder) {
        hasLength(text, placeholder.getPattern(), placeholder.getArgs());
    }

    public static void hasLength(@Nullable String text, String message, Object... args) {
        isTrue(StringUtils.hasLength(text), message, args);
    }

    public static void hasLength(@Nullable String text, Supplier<String> messageSupplier) {
        isTrue(StringUtils.hasLength(text), messageSupplier);
    }

    public static void hasText(@Nullable String text, String message, Object... args) {
        isTrue(StringUtils.hasText(text), message, args);
    }

    public static void hasText(@Nullable String text, Supplier<String> messageSupplier) {
        isTrue(StringUtils.hasText(text), messageSupplier);
    }

    public static void doesNotContain(@Nullable String textToSearch, String substring, String message, Object... args) {
        isFalse(StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) && textToSearch.contains(substring), message, args);
    }

    public static void doesNotContain(@Nullable String textToSearch, String substring, Supplier<String> messageSupplier) {
        isFalse(StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) && textToSearch.contains(substring), messageSupplier);
    }

    public static void notEmpty(@Nullable Object[] array, MessagePlaceholder placeholder) {
        notEmpty(array, placeholder.getPattern(), placeholder.getArgs());
    }

    public static void notEmpty(@Nullable Object[] array, String message, Object... args) {
        isFalse(ObjectUtils.isEmpty(array), message, args);
    }

    public static void notEmpty(@Nullable Object[] array, Supplier<String> messageSupplier) {
        isFalse(ObjectUtils.isEmpty(array), messageSupplier);
    }

    public static void noNullElements(@Nullable Object[] array, String message, Object... args) {
        if (array != null) {
            for(Object element : array) {
                if (element == null) {
                    throw BaseException.common(MessagePlaceholder.of(message, args));
                }
            }
        }

    }

    public static void noNullElements(@Nullable Object[] array, Supplier<String> messageSupplier) {
        if (array != null) {
            for(Object element : array) {
                if (element == null) {
                    throw BaseException.common(nullSafeGet(messageSupplier));
                }
            }
        }

    }

    public static void notEmpty(@Nullable Collection<?> collection, MessagePlaceholder placeholder) {
        notEmpty(collection, placeholder.getPattern(), placeholder.getArgs());
    }

    public static void notEmpty(@Nullable Collection<?> collection, String message, Object... args) {
        isFalse(CollectionUtils.isEmpty(collection), message, args);
    }

    public static void notEmpty(@Nullable Collection<?> collection, Supplier<String> messageSupplier) {
        isFalse(CollectionUtils.isEmpty(collection), messageSupplier);
    }

    public static void noNullElements(@Nullable Collection<?> collection, String message, Object... args) {
        if (collection != null) {
            for(Object element : collection) {
                if (element == null) {
                    throw BaseException.common(MessagePlaceholder.of(message, args));
                }
            }
        }

    }

    public static void noNullElements(@Nullable Collection<?> collection, Supplier<String> messageSupplier) {
        if (collection != null) {
            for(Object element : collection) {
                if (element == null) {
                    throw BaseException.common(nullSafeGet(messageSupplier));
                }
            }
        }

    }

    public static void notEmpty(@Nullable Map<?, ?> map, MessagePlaceholder placeholder) {
        notEmpty(map, placeholder.getPattern(), placeholder.getArgs());
    }

    public static void notEmpty(@Nullable Map<?, ?> map, String message, Object... args) {
        isFalse(CollectionUtils.isEmpty(map), message, args);
    }

    public static void notEmpty(@Nullable Map<?, ?> map, Supplier<String> messageSupplier) {
        isFalse(CollectionUtils.isEmpty(map), messageSupplier);
    }

    public static void isInstanceOf(Class<?> type, @Nullable Object obj, String message) {
        notNull(type, "Type to check against must not be null");
        if (!type.isInstance(obj)) {
            instanceCheckFailed(type, obj, message);
        }

    }

    public static void isInstanceOf(Class<?> type, @Nullable Object obj, Supplier<String> messageSupplier) {
        notNull(type, "Type to check against must not be null");
        if (!type.isInstance(obj)) {
            instanceCheckFailed(type, obj, (String)nullSafeGet(messageSupplier));
        }

    }

    public static void isAssignable(Class<?> superType, @Nullable Class<?> subType, String message) {
        notNull(superType, "Super type to check against must not be null");
        if (subType == null || !superType.isAssignableFrom(subType)) {
            assignableCheckFailed(superType, subType, message);
        }

    }

    public static void isAssignable(Class<?> superType, @Nullable Class<?> subType, Supplier<String> messageSupplier) {
        notNull(superType, "Super type to check against must not be null");
        if (subType == null || !superType.isAssignableFrom(subType)) {
            assignableCheckFailed(superType, subType, (String)nullSafeGet(messageSupplier));
        }

    }

    public static void isAssignable(Class<?> superType, Class<?> subType) {
        isAssignable(superType, subType, "");
    }

    private static void instanceCheckFailed(Class<?> type, @Nullable Object obj, @Nullable String msg) {
        String className = obj != null ? obj.getClass().getName() : "null";
        String result = "";
        boolean defaultMessage = true;
        if (StringUtils.hasLength(msg)) {
            if (endsWithSeparator(msg)) {
                result = msg + " ";
            } else {
                result = messageWithTypeName(msg, className);
                defaultMessage = false;
            }
        }

        if (defaultMessage) {
            result = result + "Object of class [" + className + "] must be an instance of " + type;
        }

        throw BaseException.common(result);
    }

    private static void assignableCheckFailed(Class<?> superType, @Nullable Class<?> subType, @Nullable String msg) {
        String result = "";
        boolean defaultMessage = true;
        if (StringUtils.hasLength(msg)) {
            if (endsWithSeparator(msg)) {
                result = msg + " ";
            } else {
                result = messageWithTypeName(msg, subType);
                defaultMessage = false;
            }
        }

        if (defaultMessage) {
            result = result + subType + " is not assignable to " + superType;
        }

        throw BaseException.common(result);
    }

    private static boolean endsWithSeparator(String msg) {
        return msg.endsWith(":") || msg.endsWith(";") || msg.endsWith(",") || msg.endsWith(".");
    }

    private static String messageWithTypeName(String msg, @Nullable Object typeName) {
        return msg + (msg.endsWith(" ") ? "" : ": ") + typeName;
    }

    @Nullable
    private static <T> T nullSafeGet(@Nullable Supplier<T> messageSupplier) {
        return (T)(messageSupplier != null ? messageSupplier.get() : null);
    }

    /**
     * 断言字符串匹配指定的正则表达式
     *
     * @param text 待检查的字符串
     * @param regex 正则表达式
     * @param message 不匹配时的错误消息
     * @param args 错误消息参数
     * @throws BaseException 当字符串不匹配正则表达式时
     */
    public static void matchesPattern(@Nullable String text, String regex, String message, Object... args) {
        notNull(regex, "正则表达式不能为空");
        isTrue(text != null && Pattern.matches(regex, text), message, args);
    }

    /**
     * 断言字符串匹配指定的正则表达式
     *
     * @param text 待检查的字符串
     * @param regex 正则表达式
     * @param placeholder 不匹配时的错误消息占位符
     * @throws BaseException 当字符串不匹配正则表达式时
     */
    public static void matchesPattern(@Nullable String text, String regex, MessagePlaceholder placeholder) {
        matchesPattern(text, regex, placeholder.getPattern(), placeholder.getArgs());
    }

    /**
     * 断言字符串匹配指定的正则表达式
     *
     * @param text 待检查的字符串
     * @param pattern 已编译的正则表达式模式
     * @param message 不匹配时的错误消息
     * @param args 错误消息参数
     * @throws BaseException 当字符串不匹配正则表达式时
     */
    public static void matchesPattern(@Nullable String text, Pattern pattern, String message, Object... args) {
        notNull(pattern, "正则表达式模式不能为空");
        isTrue(text != null && pattern.matcher(text).matches(), message, args);
    }

    /**
     * 断言数值在指定范围内
     *
     * @param value 待检查的数值
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @param message 不在范围内时的错误消息
     * @param args 错误消息参数
     * @throws BaseException 当数值不在指定范围内时
     */
    public static void isInRange(int value, int min, int max, String message, Object... args) {
        isTrue(value >= min && value <= max, message, args);
    }

    /**
     * 断言数值在指定范围内
     *
     * @param value 待检查的数值
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @param placeholder 不在范围内时的错误消息占位符
     * @throws BaseException 当数值不在指定范围内时
     */
    public static void isInRange(int value, int min, int max, MessagePlaceholder placeholder) {
        isInRange(value, min, max, placeholder.getPattern(), placeholder.getArgs());
    }

    /**
     * 断言数值在指定范围内（长整型）
     *
     * @param value 待检查的数值
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @param message 不在范围内时的错误消息
     * @param args 错误消息参数
     * @throws BaseException 当数值不在指定范围内时
     */
    public static void isInRange(long value, long min, long max, String message, Object... args) {
        isTrue(value >= min && value <= max, message, args);
    }

    /**
     * 断言数值在指定范围内（浮点型）
     *
     * @param value 待检查的数值
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @param message 不在范围内时的错误消息
     * @param args 错误消息参数
     * @throws BaseException 当数值不在指定范围内时
     */
    public static void isInRange(double value, double min, double max, String message, Object... args) {
        isTrue(value >= min && value <= max, message, args);
    }

    /**
     * 断言字符串包含指定的子串
     *
     * @param text 待检查的字符串
     * @param substring 期望包含的子串
     * @param message 不包含时的错误消息
     * @param args 错误消息参数
     * @throws BaseException 当字符串不包含指定子串时
     */
    public static void contains(@Nullable String text, String substring, String message, Object... args) {
        notNull(substring, "子串不能为空");
        isTrue(StringUtils.hasLength(text) && text.contains(substring), message, args);
    }

    /**
     * 断言字符串包含指定的子串
     *
     * @param text 待检查的字符串
     * @param substring 期望包含的子串
     * @param placeholder 不包含时的错误消息占位符
     * @throws BaseException 当字符串不包含指定子串时
     */
    public static void contains(@Nullable String text, String substring, MessagePlaceholder placeholder) {
        contains(text, substring, placeholder.getPattern(), placeholder.getArgs());
    }

    /**
     * 断言集合中包含指定的元素
     *
     * @param collection 待检查的集合
     * @param element 期望包含的元素
     * @param message 不包含时的错误消息
     * @param args 错误消息参数
     * @throws BaseException 当集合不包含指定元素时
     */
    public static void contains(@Nullable Collection<?> collection, Object element, String message, Object... args) {
        notNull(element, "元素不能为空");
        isTrue(collection != null && collection.contains(element), message, args);
    }

    /**
     * 断言集合中包含指定的元素
     *
     * @param collection 待检查的集合
     * @param element 期望包含的元素
     * @param placeholder 不包含时的错误消息占位符
     * @throws BaseException 当集合不包含指定元素时
     */
    public static void contains(@Nullable Collection<?> collection, Object element, MessagePlaceholder placeholder) {
        contains(collection, element, placeholder.getPattern(), placeholder.getArgs());
    }

    /**
     * 断言字符串长度不超过最大长度
     *
     * @param text 待检查的字符串
     * @param maxLength 最大长度
     * @param message 超长时的错误消息
     * @param args 错误消息参数
     * @throws BaseException 当字符串长度超过最大长度时
     */
    public static void maxLength(@Nullable String text, int maxLength, String message, Object... args) {
        isTrue(text == null || text.length() <= maxLength, message, args);
    }

    /**
     * 断言字符串长度不超过最大长度
     *
     * @param text 待检查的字符串
     * @param maxLength 最大长度
     * @param placeholder 超长时的错误消息占位符
     * @throws BaseException 当字符串长度超过最大长度时
     */
    public static void maxLength(@Nullable String text, int maxLength, MessagePlaceholder placeholder) {
        maxLength(text, maxLength, placeholder.getPattern(), placeholder.getArgs());
    }

    /**
     * 断言字符串长度不小于最小长度
     *
     * @param text 待检查的字符串
     * @param minLength 最小长度
     * @param message 长度不足时的错误消息
     * @param args 错误消息参数
     * @throws BaseException 当字符串长度小于最小长度时
     */
    public static void minLength(@Nullable String text, int minLength, String message, Object... args) {
        isTrue(text != null && text.length() >= minLength, message, args);
    }

    /**
     * 断言字符串长度不小于最小长度
     *
     * @param text 待检查的字符串
     * @param minLength 最小长度
     * @param placeholder 长度不足时的错误消息占位符
     * @throws BaseException 当字符串长度小于最小长度时
     */
    public static void minLength(@Nullable String text, int minLength, MessagePlaceholder placeholder) {
        minLength(text, minLength, placeholder.getPattern(), placeholder.getArgs());
    }
}
