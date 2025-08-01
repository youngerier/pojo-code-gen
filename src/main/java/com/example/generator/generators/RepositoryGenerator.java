package com.example.generator.generators;

import com.example.generator.CodeGenerator;
import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;

/**
 * Repository接口生成器 - 基于MyBatis Flex
 */
public class RepositoryGenerator implements CodeGenerator {

    @Override
    public TypeSpec generate(PojoInfo pojoInfo) {
        // 获取实体类名
        String entityName = pojoInfo.getClassName();
        // 创建实体类类型
        ClassName entityType = ClassName.get(pojoInfo.getPackageName(), entityName);
        // 创建MyBatis Flex的BaseMapper接口类型
        ClassName baseMapperType = ClassName.get("com.mybatisflex.core", "BaseMapper");
        // 创建参数化的BaseMapper类型
        ParameterizedTypeName baseMapperOfEntity = ParameterizedTypeName.get(baseMapperType, entityType);

        // 创建接口构建器
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(baseMapperOfEntity);

        // 添加接口注释
        if (pojoInfo.getClassComment() != null && !pojoInfo.getClassComment().isEmpty()) {
            interfaceBuilder.addJavadoc(pojoInfo.getClassComment() + "\n");
            interfaceBuilder.addJavadoc("数据访问层接口\n");
        }

        return interfaceBuilder.build();
    }

    @Override
    public String getPackageName(PojoInfo pojoInfo) {
        return pojoInfo.getPackageName().replace(".entity", ".repository");
    }

    @Override
    public String getClassName(PojoInfo pojoInfo) {
        return pojoInfo.getClassName() + "Repository";
    }
}