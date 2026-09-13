package io.github.youngerier.generator.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.GeneratedType;
import io.github.youngerier.generator.model.PackageStructure;

import javax.lang.model.element.Modifier;

/**
 * "字段平铺型"模型类（DTO、Request、Response、Query）生成器骨架：
 * 统一 {@code @Data} 公共类声明、实体字段遍历、字段注释与类注释的处理，
 * 差异部分通过钩子方法由子类提供。
 */
abstract class AbstractModelGenerator extends BaseGenerator {

    private static final ClassName LOMBOK_DATA = ClassName.get("lombok", "Data");

    private final String docLabel;

    protected AbstractModelGenerator(PackageStructure packages, GeneratedType outputType, String docLabel) {
        super(packages, outputType);
        this.docLabel = docLabel;
    }

    @Override
    public TypeSpec generate(ClassMetadata metadata) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(getClassName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(LOMBOK_DATA);

        customize(metadata, builder);

        for (ClassMetadata.FieldInfo field : metadata.getFields()) {
            if (includeField(field)) {
                builder.addField(buildField(field));
            }
        }

        appendExtraFields(metadata, builder);
        Javadocs.appendClassComment(builder, metadata, docLabel);
        return builder.build();
    }

    /**
     * 字段遍历前的自定义钩子，例如声明父类。
     */
    protected void customize(ClassMetadata metadata, TypeSpec.Builder builder) {
    }

    /**
     * 字段是否需要生成，默认全部生成。
     */
    protected boolean includeField(ClassMetadata.FieldInfo field) {
        return true;
    }

    /**
     * 实体字段之后追加额外字段，例如 Query 的时间范围查询字段。
     */
    protected void appendExtraFields(ClassMetadata metadata, TypeSpec.Builder builder) {
    }

    private FieldSpec buildField(ClassMetadata.FieldInfo field) {
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(field.getType(), field.getName(), Modifier.PRIVATE);
        String comment = field.getComment();
        if (comment != null && !comment.isEmpty()) {
            fieldBuilder.addJavadoc(comment + "\n");
        }
        appendFieldJavadoc(field, fieldBuilder);
        return fieldBuilder.build();
    }

    /**
     * 在字段自身注释之后追加说明，例如主键标记。
     */
    protected void appendFieldJavadoc(ClassMetadata.FieldInfo field, FieldSpec.Builder fieldBuilder) {
    }
}
