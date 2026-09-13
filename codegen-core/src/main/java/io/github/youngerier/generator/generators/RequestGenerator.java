package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.GeneratedType;
import io.github.youngerier.generator.model.PackageStructure;

import java.util.Set;

/**
 * Request 模型类生成器
 */
public class RequestGenerator extends AbstractModelGenerator {

    /**
     * 通常不需要包含在请求对象中的字段名（主键、审计时间字段）。
     */
    private static final Set<String> EXCLUDED_FIELDS =
            Set.of("id", "gmtCreate", "gmtModified", "createTime", "updateTime", "createdAt", "updatedAt");

    public RequestGenerator(PackageStructure packageStructure) {
        super(packageStructure, GeneratedType.REQUEST, "请求参数对象");
    }

    @Override
    protected boolean includeField(ClassMetadata.FieldInfo field) {
        return !EXCLUDED_FIELDS.contains(field.getName());
    }
}
