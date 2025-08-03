package com.example.generator.generators;

import com.example.generator.CodeGenerator;
import com.example.generator.model.PackageConfig;
import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.element.Modifier;

/**
 * Query模型类生成器
 */
@Slf4j
public class QueryGenerator implements CodeGenerator {
    private final PackageConfig packageConfig;

    public QueryGenerator(PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
    }

    @Override
    public TypeSpec generate(PojoInfo pojoInfo) {
        // 创建类构建器
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("lombok", "Data"));

        // 添加字段
        for (PojoInfo.FieldInfo field : pojoInfo.getFields()) {
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
        if (pojoInfo.getClassComment() != null && !pojoInfo.getClassComment().isEmpty()) {
            classBuilder.addJavadoc(pojoInfo.getClassComment() + "\n");
            classBuilder.addJavadoc("查询参数对象\n");
        }

        return classBuilder.build();
    }

    @Override
    public String getPackageName() {
        return packageConfig.getRequestPackage();
    }

    @Override
    public String getClassName(PojoInfo pojoInfo) {
        return pojoInfo.getClassName() + "Query";
    }
}
