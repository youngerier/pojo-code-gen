package com.example.generator.generators;

import com.example.generator.CodeGenerator;
import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.*;
import javax.lang.model.element.Modifier;

/**
 * DTO类生成器
 */
public class DtoGenerator implements CodeGenerator {

    @Override
    public TypeSpec generate(PojoInfo pojoInfo) {
        // 创建类构建器
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("lombok", "Data"));

        // 添加类注释
        if (pojoInfo.getClassComment() != null && !pojoInfo.getClassComment().isEmpty()) {
            classBuilder.addJavadoc(pojoInfo.getClassComment() + "\n");
            classBuilder.addJavadoc("数据传输对象(DTO)\n");
        }

        // 添加字段
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
    public String getPackageName(PojoInfo pojoInfo) {
        // 假设DTO包在原POJO包的同级dto目录
        return pojoInfo.getPackageName().replace(".pojo", ".dto");
    }

    @Override
    public String getClassName(PojoInfo pojoInfo) {
        return pojoInfo.getClassName() + "DTO";
    }
}
