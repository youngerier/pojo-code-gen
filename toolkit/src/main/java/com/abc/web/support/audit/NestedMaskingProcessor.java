package com.abc.web.support.audit;

import com.abc.web.support.audit.annotations.SensitiveField;
import com.abc.web.support.audit.annotations.SensitiveParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 嵌套对象脱敏处理器
 * 支持对复杂对象内部属性进行脱敏处理
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Slf4j
public class NestedMaskingProcessor {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    // 缓存已分析的类结构，避免重复反射
    private static final Map<Class<?>, List<SensitiveFieldInfo>> CLASS_FIELD_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 对复杂对象进行嵌套脱敏
     * 
     * @param data 原始数据
     * @param strategy 脱敏策略
     * @param customExpression 自定义表达式
     * @param fieldPaths 指定字段路径
     * @param autoNested 是否自动嵌套处理
     * @return 脱敏后的数据
     */
    public static Object maskNestedData(Object data, 
                                       SensitiveParam.MaskStrategy strategy,
                                       String customExpression,
                                       String[] fieldPaths,
                                       boolean autoNested) {
        if (data == null) {
            return null;
        }
        
        // 如果是基本类型或字符串，直接脱敏
        if (isSimpleType(data.getClass())) {
            return DataMaskingUtils.maskData(data, strategy, customExpression);
        }
        
        try {
            // 将对象转换为JSON进行处理
            JsonNode jsonNode = OBJECT_MAPPER.valueToTree(data);
            if (!jsonNode.isObject()) {
                return DataMaskingUtils.maskData(data, strategy, customExpression);
            }
            
            ObjectNode objectNode = (ObjectNode) jsonNode;
            
            // 处理指定字段路径
            if (fieldPaths.length > 0) {
                for (String fieldPath : fieldPaths) {
                    maskFieldByPath(objectNode, fieldPath, strategy, customExpression);
                }
            }
            
            // 自动嵌套处理
            if (autoNested) {
                maskObjectByAnnotations(objectNode, data.getClass());
            }
            
            return OBJECT_MAPPER.treeToValue(objectNode, Object.class);
            
        } catch (Exception e) {
            log.warn("嵌套脱敏处理失败，使用默认脱敏: {}", e.getMessage());
            return DataMaskingUtils.maskData(data.toString(), SensitiveParam.MaskStrategy.DEFAULT, null);
        }
    }
    
    /**
     * 根据字段路径进行脱敏
     */
    private static void maskFieldByPath(ObjectNode objectNode, String fieldPath, 
                                       SensitiveParam.MaskStrategy strategy, String customExpression) {
        String[] pathParts = fieldPath.split("\\.");
        JsonNode currentNode = objectNode;
        
        // 导航到目标字段的父节点
        for (int i = 0; i < pathParts.length - 1; i++) {
            currentNode = currentNode.get(pathParts[i]);
            if (currentNode == null || !currentNode.isObject()) {
                return; // 路径不存在或类型不匹配
            }
        }
        
        // 脱敏目标字段
        String targetField = pathParts[pathParts.length - 1];
        JsonNode targetValue = currentNode.get(targetField);
        if (targetValue != null && !targetValue.isNull()) {
            Object maskedValue = DataMaskingUtils.maskData(targetValue.asText(), strategy, customExpression);
            ((ObjectNode) currentNode).put(targetField, maskedValue.toString());
        }
    }
    
    /**
     * 根据注解自动处理对象脱敏
     */
    private static void maskObjectByAnnotations(ObjectNode objectNode, Class<?> clazz) {
        List<SensitiveFieldInfo> sensitiveFields = getSensitiveFields(clazz);
        
        for (SensitiveFieldInfo fieldInfo : sensitiveFields) {
            JsonNode fieldValue = objectNode.get(fieldInfo.fieldName);
            if (fieldValue == null || fieldValue.isNull()) {
                continue;
            }
            
            if (fieldValue.isValueNode()) {
                // 简单值类型，直接脱敏
                Object maskedValue = DataMaskingUtils.maskData(
                    fieldValue.asText(), 
                    fieldInfo.strategy, 
                    fieldInfo.customExpression
                );
                objectNode.put(fieldInfo.fieldName, maskedValue.toString());
            } else if (fieldValue.isObject() && fieldInfo.enableNested) {
                // 复杂对象，递归处理
                maskObjectByAnnotations((ObjectNode) fieldValue, fieldInfo.fieldType);
            } else if (fieldValue.isArray()) {
                // 数组类型，处理每个元素
                // TODO: 如需支持数组脱敏，可在此扩展
            }
        }
    }
    
    /**
     * 获取类中标记的敏感字段信息
     */
    private static List<SensitiveFieldInfo> getSensitiveFields(Class<?> clazz) {
        return CLASS_FIELD_CACHE.computeIfAbsent(clazz, key -> {
            List<SensitiveFieldInfo> result = new ArrayList<>();
            
            ReflectionUtils.doWithFields(clazz, field -> {
                SensitiveField annotation = field.getAnnotation(SensitiveField.class);
                if (annotation != null) {
                    result.add(new SensitiveFieldInfo(
                        field.getName(),
                        field.getType(),
                        annotation.strategy(),
                        annotation.customExpression(),
                        annotation.enableNested()
                    ));
                }
            });
            
            return result;
        });
    }
    
    /**
     * 判断是否为简单类型
     */
    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == String.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Double.class ||
               clazz == Float.class ||
               clazz == Boolean.class ||
               clazz == Character.class ||
               clazz == Byte.class ||
               clazz == Short.class ||
               Number.class.isAssignableFrom(clazz) ||
               CharSequence.class.isAssignableFrom(clazz);
    }
    
    /**
     * 敏感字段信息
     */
    private static class SensitiveFieldInfo {
        final String fieldName;
        final Class<?> fieldType;
        final SensitiveParam.MaskStrategy strategy;
        final String customExpression;
        final boolean enableNested;
        
        SensitiveFieldInfo(String fieldName, Class<?> fieldType, 
                          SensitiveParam.MaskStrategy strategy, String customExpression, 
                          boolean enableNested) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.strategy = strategy;
            this.customExpression = customExpression;
            this.enableNested = enableNested;
        }
    }
}