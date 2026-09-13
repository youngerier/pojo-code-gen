package io.github.youngerier.generator.generators;

import com.squareup.javapoet.FieldSpec;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.GeneratedType;
import io.github.youngerier.generator.model.PackageStructure;

/**
 * DTO 类生成器
 */
public class DtoGenerator extends AbstractModelGenerator {

    public DtoGenerator(PackageStructure packageStructure) {
        super(packageStructure, GeneratedType.DTO, "数据传输对象(DTO)");
    }

    @Override
    protected void appendFieldJavadoc(ClassMetadata.FieldInfo field, FieldSpec.Builder fieldBuilder) {
        if (field.isPrimaryKey()) {
            fieldBuilder.addJavadoc("主键ID\n");
        }
    }
}
