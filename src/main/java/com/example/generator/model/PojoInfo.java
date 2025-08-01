package com.example.generator.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 封装从POJO解析出的信息
 */
@Data
public class PojoInfo {
    private String packageName; // 包名
    private String className;   // 类名
    private String classComment; // 类注释
    private List<FieldInfo> fields = new ArrayList<>(); // 字段信息列表

    /**
     * 字段信息内部类
     */
    @Data
    public static class FieldInfo {
        private String name;         // 字段名
        private String type;         // 字段类型
        private String fullType;     // 字段完整类型
        private String comment;      // 字段注释
        private boolean isPrimaryKey; // 是否为主键
    }
}