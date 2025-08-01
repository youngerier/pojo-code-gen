package com.example.generator.generators;

import com.example.generator.CodeGenerator;
import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.*;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;

/**
 * Response模型类生成器
 */
public class ResponseGenerator implements CodeGenerator {

    @Override
    public TypeSpec generate(PojoInfo pojoInfo) {
        // 创建类构建器
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("lombok", "Data"));

        // 添加所有字段
        for (PojoInfo.FieldInfo field : pojoInfo.getFields()) {
            // 创建字段类型
            TypeName fieldType;
            try {
                // 处理基本类型和包装类型
                fieldType = ClassName.bestGuess(field.getType());
            } catch (IllegalArgumentException e) {
                // 处理无法识别的类型（使用Object）
                fieldType = TypeName.OBJECT;
            }
            
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
            classBuilder.addJavadoc("响应对象\n");
        }

        return classBuilder.build();
    }

    @Override
    public String getPackageName(PojoInfo pojoInfo) {
        return pojoInfo.getPackageName().replace(".entity", ".model.response");
    }

    @Override
    public String getClassName(PojoInfo pojoInfo) {
        return pojoInfo.getClassName() + "Response";
    }
}