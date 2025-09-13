package io.github.youngerier.support;

import io.github.youngerier.support.enums.I18nCommonExceptionCode;
import io.github.youngerier.support.exception.I18nExceptionCode;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 断言工具类
 * 专门用于参数验证和业务断言
 */
public class Assert {

    // 常用正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^1[3-9]\\d{9}$");
    
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
            "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$");
    
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?)://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?$");
    
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    // ==================== 基础断言 ====================

    /**
     * 断言对象非空
     *
     * @param obj       对象
     * @param paramName 参数名
     * @throws BusinessException 如果对象为空
     */
    public static void notNull(Object obj, String paramName) {
        ExceptionUtils.throwIf(obj == null, I18nCommonExceptionCode.PARAM_REQUIRED, paramName);
    }

    /**
     * 断言字符串非空
     *
     * @param str       字符串
     * @param paramName 参数名
     * @throws BusinessException 如果字符串为空
     */
    public static void notBlank(String str, String paramName) {
        ExceptionUtils.throwIf(!StringUtils.hasText(str), I18nCommonExceptionCode.PARAM_REQUIRED, paramName);
    }

    /**
     * 断言集合非空
     *
     * @param collection 集合
     * @param paramName  参数名
     * @throws BusinessException 如果集合为空
     */
    public static void notEmpty(Collection<?> collection, String paramName) {
        ExceptionUtils.throwIf(collection == null || collection.isEmpty(), 
                              I18nCommonExceptionCode.PARAM_REQUIRED, paramName);
    }

    /**
     * 断言条件为真
     *
     * @param condition     条件
     * @param exceptionCode 异常码
     * @throws BusinessException 如果条件为假
     */
    public static void isTrue(boolean condition, I18nExceptionCode exceptionCode) {
        ExceptionUtils.throwIf(!condition, exceptionCode);
    }

    /**
     * 断言条件为真（带参数）
     *
     * @param condition     条件
     * @param exceptionCode 异常码
     * @param args          参数
     * @throws BusinessException 如果条件为假
     */
    public static void isTrue(boolean condition, I18nExceptionCode exceptionCode, Object... args) {
        ExceptionUtils.throwIf(!condition, exceptionCode, args);
    }

    /**
     * 断言条件为假
     *
     * @param condition     条件
     * @param exceptionCode 异常码
     * @throws BusinessException 如果条件为真
     */
    public static void isFalse(boolean condition, I18nExceptionCode exceptionCode) {
        ExceptionUtils.throwIf(condition, exceptionCode);
    }

    /**
     * 断言两个对象相等
     *
     * @param obj1          对象1
     * @param obj2          对象2
     * @param exceptionCode 异常码     * @th
rows BusinessException 如果两个对象不相等
     */
    public static void equals(Object obj1, Object obj2, I18nExceptionCode exceptionCode) {
        ExceptionUtils.throwIf(!Objects.equals(obj1, obj2), exceptionCode);
    }

    /**
     * 断言两个对象不相等
     *
     * @param obj1          对象1
     * @param obj2          对象2
     * @param exceptionCode 异常码
     * @throws BusinessException 如果两个对象相等
     */
    public static void notEquals(Object obj1, Object obj2, I18nExceptionCode exceptionCode) {
        ExceptionUtils.throwIf(Objects.equals(obj1, obj2), exceptionCode);
    }

    // ==================== 格式验证断言 ====================

    /**
     * 断言邮箱格式正确
     *
     * @param email 邮箱
     * @throws BusinessException 如果邮箱格式不正确
     */
    public static void validEmail(String email) {
        ExceptionUtils.throwIf(!StringUtils.hasText(email) || !EMAIL_PATTERN.matcher(email).matches(), 
                              I18nCommonExceptionCode.EMAIL_INVALID);
    }

    /**
     * 断言手机号格式正确
     *
     * @param phone 手机号
     * @throws BusinessException 如果手机号格式不正确
     */
    public static void validPhone(String phone) {
        ExceptionUtils.throwIf(!StringUtils.hasText(phone) || !PHONE_PATTERN.matcher(phone).matches(), 
                              I18nCommonExceptionCode.PHONE_INVALID);
    }

    /**
     * 断言身份证号格式正确
     *
     * @param idCard 身份证号
     * @throws BusinessException 如果身份证号格式不正确
     */
    public static void validIdCard(String idCard) {
        ExceptionUtils.throwIf(!StringUtils.hasText(idCard) || !ID_CARD_PATTERN.matcher(idCard).matches(), 
                              I18nCommonExceptionCode.ID_CARD_INVALID);
    }

    // ==================== 范围断言 ====================

    /**
     * 断言数值在指定范围内
     *
     * @param value     数值
     * @param min       最小值
     * @param max       最大值
     * @param paramName 参数名
     * @throws BusinessException 如果数值不在范围内
     */
    public static void inRange(Number value, Number min, Number max, String paramName) {
        notNull(value, paramName);
        double val = value.doubleValue();
        double minVal = min.doubleValue();
        double maxVal = max.doubleValue();
        
        ExceptionUtils.throwIf(val < minVal || val > maxVal, 
                              I18nCommonExceptionCode.PARAM_OUT_OF_RANGE, paramName, min, max);
    }

    /**
     * 断言字符串长度在指定范围内
     *
     * @param str       字符串
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @param paramName 参数名
     * @throws BusinessException 如果字符串长度不符合要求
     */
    public static void lengthBetween(String str, int minLength, int maxLength, String paramName) {
        notNull(str, paramName);
        int length = str.length();
        ExceptionUtils.throwIf(length < minLength || length > maxLength, 
                              I18nCommonExceptionCode.PARAM_OUT_OF_RANGE, paramName, minLength, maxLength);
    }

    // ==================== 便捷断言方法 ====================

    /**
     * 断言数据存在
     *
     * @param data 数据
     * @throws BusinessException 如果数据不存在
     */
    public static void exists(Object data) {
        ExceptionUtils.throwIf(data == null, I18nCommonExceptionCode.DATA_NOT_FOUND);
    }

    /**
     * 断言数据不存在（用于创建场景）
     *
     * @param data 数据
     * @throws BusinessException 如果数据已存在
     */
    public static void notExists(Object data) {
        ExceptionUtils.throwIf(data != null, I18nCommonExceptionCode.DATA_ALREADY_EXISTS);
    }

    /**
     * 断言有权限
     *
     * @param hasPermission 是否有权限
     * @throws BusinessException 如果没有权限
     */
    public static void hasPermission(boolean hasPermission) {
        ExceptionUtils.throwIf(!hasPermission, I18nCommonExceptionCode.FORBIDDEN);
    }

    /**
     * 断言已认证
     *
     * @param authenticated 是否已认证
     * @throws BusinessException 如果未认证
     */
    public static void authenticated(boolean authenticated) {
        ExceptionUtils.throwIf(!authenticated, I18nCommonExceptionCode.UNAUTHORIZED);
    }
    
    /**
     * 断言用户已登录
     *
     * @param user 用户对象
     * @throws BusinessException 如果用户未登录
     */
    public static void userLoggedIn(Object user) {
        ExceptionUtils.throwIf(user == null, I18nCommonExceptionCode.UNAUTHORIZED);
    }
    
    /**
     * 断言Token有效
     *
     * @param tokenValid Token是否有效
     * @throws BusinessException 如果Token无效
     */
    public static void validToken(boolean tokenValid) {
        ExceptionUtils.throwIf(!tokenValid, I18nCommonExceptionCode.TOKEN_INVALID);
    }
    
    /**
     * 断言账户未被锁定
     *
     * @param locked 账户是否被锁定
     * @throws BusinessException 如果账户被锁定
     */
    public static void accountNotLocked(boolean locked) {
        ExceptionUtils.throwIf(locked, I18nCommonExceptionCode.ACCOUNT_LOCKED);
    }
    
    /**
     * 断言账户已启用
     *
     * @param enabled 账户是否已启用
     * @throws BusinessException 如果账户被禁用
     */
    public static void accountEnabled(boolean enabled) {
        ExceptionUtils.throwIf(!enabled, I18nCommonExceptionCode.USER_DISABLED);
    }
    
    /**
     * 断言密码正确
     *
     * @param passwordMatch 密码是否匹配
     * @throws BusinessException 如果密码错误
     */
    public static void correctPassword(boolean passwordMatch) {
        ExceptionUtils.throwIf(!passwordMatch, I18nCommonExceptionCode.PASSWORD_INCORRECT);
    }
    
    /**
     * 断言特定权限
     *
     * @param hasPermission 是否有权限
     * @param resource 资源名称
     * @throws BusinessException 如果没有权限
     */
    public static void hasPermissionFor(boolean hasPermission, String resource) {
        ExceptionUtils.throwIf(!hasPermission, I18nCommonExceptionCode.PERMISSION_DENIED, resource);
    }
}