package io.github.youngerier.generator.model;

/**
 * 代码生成产物类型，统一描述每类产物的包名后缀与类名后缀。
 */
public enum GeneratedType {

    DTO(".model.dto", "DTO"),
    SERVICE(".service", "Service"),
    SERVICE_IMPL(".service.impl", "ServiceImpl"),
    REPOSITORY(".dal.repository", "Repository"),
    MAPPER(".dal.mapper", "Mapper"),
    REQUEST(".model.request", "Request"),
    QUERY(".model.request", "Query"),
    RESPONSE(".model.response", "Response"),
    CONVERTOR(".convertor", "Convertor"),
    CONTROLLER(".controller", "Controller");

    private final String packageSuffix;
    private final String classSuffix;

    GeneratedType(String packageSuffix, String classSuffix) {
        this.packageSuffix = packageSuffix;
        this.classSuffix = classSuffix;
    }

    public String packageName(String basePackage) {
        return basePackage + packageSuffix;
    }

    public String className(String entityName) {
        return entityName + classSuffix;
    }
}
