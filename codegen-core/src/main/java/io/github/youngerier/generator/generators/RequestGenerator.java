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
     * 通常不由请求方传入的审计时间字段（各命名风格）。
     */
    private static final Set<String> AUDIT_FIELDS =
            Set.of("gmtCreate", "gmtModified", "createTime", "updateTime", "createdAt", "updatedAt");

    public RequestGenerator(PackageStructure packageStructure) {
        super(packageStructure, GeneratedType.REQUEST, "请求参数对象");
    }

    /**
     * 主键（按 {@code @Id} 识别，而非固定字段名 "id"）与审计时间字段不进入请求对象。
     */
    @Override
    protected boolean includeField(ClassMetadata.FieldInfo field) {
        return !field.isPrimaryKey() && !AUDIT_FIELDS.contains(field.getName());
    }
}
