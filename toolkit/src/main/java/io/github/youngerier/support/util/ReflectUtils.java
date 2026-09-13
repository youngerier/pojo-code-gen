package io.github.youngerier.support.util;

import io.github.youngerier.support.AssertUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 轻量反射工具：递归获取类（含父类）的实例字段与 getter 方法，结果按 Class 缓存。
 */
public final class ReflectUtils {

    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentReferenceHashMap<>();

    private ReflectUtils() {
        throw new AssertionError();
    }

    /**
     * 获取类及其父类的所有实例字段（排除静态字段）。
     */
    public static Field[] getFields(Class<?> clazz) {
        AssertUtils.notNull(clazz, "argument clazz must not null");
        return memberFields(clazz).toArray(Field[]::new);
    }

    /**
     * 获取类的所有 public getter 方法（含父类，符合 JavaBean 规范）。
     */
    public static Method[] getGetterMethods(Class<?> clazz) {
        AssertUtils.notNull(clazz, "argument clazz must not null");
        return Arrays.stream(clazz.getMethods())
                .filter(ReflectUtils::isGetter)
                .toArray(Method[]::new);
    }

    private static List<Field> memberFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, ReflectUtils::collectFields).stream()
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toList();
    }

    private static List<Field> collectFields(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return Collections.emptyList();
        }
        List<Field> fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        fields.addAll(collectFields(clazz.getSuperclass()));
        return fields;
    }

    private static boolean isGetter(Method method) {
        String name = method.getName();
        if ("getClass".equals(name)) {
            return false;
        }
        boolean getter = name.startsWith("get") && name.length() > 3;
        boolean booleanGetter = name.startsWith("is") && name.length() > 2;
        return Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && method.getParameterCount() == 0
                && method.getReturnType() != void.class
                && (getter || booleanGetter);
    }
}
