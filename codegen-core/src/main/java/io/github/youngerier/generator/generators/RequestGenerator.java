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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Request模型类生成器
 */
@Slf4j
public class RequestGenerator implements CodeGenerator {
    private final PackageStructure packageStructure;

    public RequestGenerator(PackageStructure packageStructure) {
        this.packageStructure = packageStructure;
    }

    // 通常不需要包含在请求对象中的字段名
    private static final Set<String> EXCLUDED_FIELDS = new HashSet<>(Arrays.asList(
            "id", "gmtCreate", "gmtModified", "createTime", "updateTime", "createdAt", "updatedAt"
    ));

    @Override
    public TypeSpec generate(ClassMetadata classMetadata) {
        // 创建类构建器
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(classMetadata))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("lombok", "Data"));

        // 添加字段
        for (ClassMetadata.FieldInfo field : classMetadata.getFields()) {
            // 排除不需要的字段
            if (EXCLUDED_FIELDS.contains(field.getName())) {
                continue;
            }

            // 创建字段类型
            TypeName fieldType = ClassName.bestGuess(field.getFullType());

            FieldSpec.Builder fieldBuilder = FieldSpec.builder(
                    fieldType,
                    field.getName(),
                    Modifier.PRIVATE
            );

            // 添加字段注释
            if (field.getComment() != null && !field.getComment().isEmpty()) {
                fieldBuilder.addJavadoc(field.getComment() + "\n");
            }

            classBuilder.addField(fieldBuilder.build());
        }

        // 添加类注释
        if (classMetadata.getClassComment() != null && !classMetadata.getClassComment().isEmpty()) {
            classBuilder.addJavadoc(classMetadata.getClassComment() + "\n");
            classBuilder.addJavadoc("请求参数对象\n");
        }

        return classBuilder.build();
    }

    @Override
    public String getPackageName() {
        return packageStructure.getRequestPackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getRequestClassName(classMetadata.getClassName());
    }
}
