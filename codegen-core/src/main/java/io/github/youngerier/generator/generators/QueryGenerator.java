package io.github.youngerier.generator.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.youngerier.support.enums.DefaultOrderField;
import io.github.youngerier.support.page.AbstractPageQuery;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.GeneratedType;
import io.github.youngerier.generator.model.PackageStructure;

import javax.lang.model.element.Modifier;

/**
 * Query 模型类生成器
 */
public class QueryGenerator extends AbstractModelGenerator {

    private static final ClassName LOCAL_DATE_TIME = ClassName.get("java.time", "LocalDateTime");

    public QueryGenerator(PackageStructure packageStructure) {
        super(packageStructure, GeneratedType.QUERY, "查询参数对象");
    }

    @Override
    protected void customize(ClassMetadata metadata, TypeSpec.Builder builder) {
        builder.superclass(ParameterizedTypeName.get(
                ClassName.get(AbstractPageQuery.class), ClassName.get(DefaultOrderField.class)));
    }

    /**
     * Collection/Map 字段不是映射列，不能作为等值查询条件。
     */
    @Override
    protected boolean includeField(ClassMetadata.FieldInfo field) {
        return field.isColumn();
    }

    /**
     * 时间范围查询字段：仅在实体存在对应审计字段时生成。
     */
    @Override
    protected void appendExtraFields(ClassMetadata metadata, TypeSpec.Builder builder) {
        if (hasField(metadata, "gmtCreate")) {
            builder.addField(timeRangeField("minGmtCreate", "最小创建时间"));
            builder.addField(timeRangeField("maxGmtCreate", "最大创建时间"));
        }
        if (hasField(metadata, "gmtModified")) {
            builder.addField(timeRangeField("minGmtModified", "最小修改时间"));
            builder.addField(timeRangeField("maxGmtModified", "最大修改时间"));
        }
    }

    private static boolean hasField(ClassMetadata metadata, String fieldName) {
        return metadata.getFields().stream().anyMatch(field -> fieldName.equals(field.getName()));
    }

    private FieldSpec timeRangeField(String name, String comment) {
        return FieldSpec.builder(LOCAL_DATE_TIME, name, Modifier.PRIVATE)
                .addJavadoc(Javadocs.escapeLiteral(comment) + "\n")
                .build();
    }
}
