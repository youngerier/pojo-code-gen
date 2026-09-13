package io.github.youngerier.generator.model;

import com.squareup.javapoet.ClassName;
import lombok.Getter;

/**
 * 包结构配置，基于实体所在的基础包名与实体名，按标准约定推导各类产物的包路径和类名。
 */
@Getter
public class PackageStructure {

    private final String basePackage;
    private final String entityName;

    public PackageStructure(String basePackage, String entityName) {
        this.basePackage = basePackage;
        this.entityName = entityName;
    }

    /**
     * 获取指定产物类型对应的 {@link ClassName}。
     */
    public ClassName type(GeneratedType type) {
        return ClassName.get(type.packageName(basePackage), type.className(entityName));
    }

    public ClassName dto() {
        return type(GeneratedType.DTO);
    }

    public ClassName service() {
        return type(GeneratedType.SERVICE);
    }

    public ClassName serviceImpl() {
        return type(GeneratedType.SERVICE_IMPL);
    }

    public ClassName repository() {
        return type(GeneratedType.REPOSITORY);
    }

    public ClassName mapper() {
        return type(GeneratedType.MAPPER);
    }

    public ClassName request() {
        return type(GeneratedType.REQUEST);
    }

    public ClassName query() {
        return type(GeneratedType.QUERY);
    }

    public ClassName response() {
        return type(GeneratedType.RESPONSE);
    }

    public ClassName convertor() {
        return type(GeneratedType.CONVERTOR);
    }

    public ClassName controller() {
        return type(GeneratedType.CONTROLLER);
    }
}
