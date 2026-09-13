package io.github.youngerier.generator.model;

import com.squareup.javapoet.TypeName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类元数据信息，包含从源码中解析出的类的详细信息
 */
@Data
public class ClassMetadata {

    private String packageName;
    private String className;
    private String classComment;
    private List<FieldInfo> fields = new ArrayList<>();

    /**
     * 去掉最后一段包名后的基础包名，例如 {@code com.abc.entity} -> {@code com.abc}。
     */
    public String getBasePackageName() {
        return packageName.substring(0, packageName.lastIndexOf('.'));
    }

    /**
     * 首字母小写的类名，例如 {@code User} -> {@code user}。
     */
    public String getCamelClassName() {
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /**
     * 字段信息
     */
    @Data
    public static class FieldInfo {
        private String name;
        private TypeName type;
        private String fullType;
        private String comment;
        private boolean primaryKey;
    }
}
