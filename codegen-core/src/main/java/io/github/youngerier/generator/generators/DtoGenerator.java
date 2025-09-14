package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.CodeGenerator;
import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.element.Modifier;

/**
 * DTO类生成器
 */
@Slf4j
public class DtoGenerator implements CodeGenerator {

    private final PackageStructure packageStructure;

    public DtoGenerator(PackageStructure packageStructure) {
        this.packageStructure = packageStructure;
    }

    @Override
    public TypeSpec generate(ClassMetadata classMetadata) {
        // 创建类构建器
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(classMetadata))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("lombok", "Data"));

        // 添加类注释
        if (classMetadata.getClassComment() != null && !classMetadata.getClassComment().isEmpty()) {
            classBuilder.addJavadoc(classMetadata.getClassComment() + "\n");
            classBuilder.addJavadoc("数据传输对象(DTO)\n");
        }

        // 添加字段
        for (ClassMetadata.FieldInfo field : classMetadata.getFields()) {
            // 创建字段类型
            TypeName fieldType = field.getType();

            // 创建字段构建器
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, field.getName(), Modifier.PRIVATE);

            // 添加字段注释
            if (field.getComment() != null && !field.getComment().isEmpty()) {
                fieldBuilder.addJavadoc(field.getComment() + "\n");
            }

            // 如果是主键，添加注释说明
            if (field.isPrimaryKey()) {
                fieldBuilder.addJavadoc("主键ID\n");
            }

            classBuilder.addField(fieldBuilder.build());
        }

        return classBuilder.build();
    }

    @Override
    public String getPackageName() {
        return packageStructure.getDtoPackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getDtoClassName();
    }
}