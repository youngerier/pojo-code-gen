package com.example.generator.generators;

import com.example.generator.CodeGenerator;
import com.example.generator.model.PackageLayout;
import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;

import com.abc.web.support.AbstractPageQuery;
import com.abc.web.support.enums.DefaultOrderField;

import javax.lang.model.element.Modifier;

/**
 * Query模型类生成器
 */
@Slf4j
public class QueryGenerator implements CodeGenerator {
    private final PackageLayout packageLayout;

    public QueryGenerator(PackageLayout packageLayout) {
        this.packageLayout = packageLayout;
    }

    @Override
    public TypeSpec generate(PojoInfo pojoInfo) {
        // 创建类构建器
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(ClassName.get(AbstractPageQuery.class), ClassName.get(DefaultOrderField.class)))
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

        // 添加时间范围查询字段
        TypeName localDateTimeType = ClassName.get("java.time", "LocalDateTime");

        FieldSpec minGmtCreate = FieldSpec.builder(localDateTimeType, "minGmtCreate", Modifier.PRIVATE)
                .addJavadoc("最小创建时间\n")
                .build();
        classBuilder.addField(minGmtCreate);

        FieldSpec maxGmtCreate = FieldSpec.builder(localDateTimeType, "maxGmtCreate", Modifier.PRIVATE)
                .addJavadoc("最大创建时间\n")
                .build();
        classBuilder.addField(maxGmtCreate);

        FieldSpec minGmtModified = FieldSpec.builder(localDateTimeType, "minGmtModified", Modifier.PRIVATE)
                .addJavadoc("最小修改时间\n")
                .build();
        classBuilder.addField(minGmtModified);

        FieldSpec maxGmtModified = FieldSpec.builder(localDateTimeType, "maxGmtModified", Modifier.PRIVATE)
                .addJavadoc("最大修改时间\n")
                .build();
        classBuilder.addField(maxGmtModified);

        return classBuilder.build();
    }

    @Override
    public String getPackageName() {
        return packageLayout.getRequestPackage();
    }

    @Override
    public String getClassName(PojoInfo pojoInfo) {
        return packageLayout.getQueryClassName(pojoInfo.getClassName());
    }
}
