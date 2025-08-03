package com.example.generator.generators;

import com.example.generator.CodeGenerator;
import com.example.generator.model.PackageConfig;
import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.MethodSpec;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Repository接口生成器 - 基于MyBatis Flex
 */
@Slf4j
public class RepositoryGenerator implements CodeGenerator {
    private final PackageConfig packageConfig;

    public RepositoryGenerator(PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
    }

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

        // 添加selectListByQuery方法
        MethodSpec selectListByQuery = MethodSpec.methodBuilder("selectListByQuery")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(ClassName.get(packageConfig.getRequestPackage(), pojoInfo.getClassName() + "Query"), "query")
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), entityType))
                .addStatement("QueryWrapper queryWrapper = QueryWrapper.create()")
                .addCode(buildQuery(pojoInfo))
                .addStatement("return selectListByQuery(queryWrapper)")
                .build();
        interfaceBuilder.addMethod(selectListByQuery);

        return interfaceBuilder.build();
    }

    private String buildQuery(PojoInfo pojoInfo) {
        StringBuilder queryBuilder = new StringBuilder();
        for (PojoInfo.FieldInfo field : pojoInfo.getFields()) {
            queryBuilder.append(String.format("if (query.get%s() != null) {\n", 
                upperFirstChar(field.getName())));
            queryBuilder.append(String.format("    queryWrapper.where(%s.%s.eq(query.get%s()));\n", 
                pojoInfo.getClassName().toUpperCase(), field.getName().toUpperCase(), upperFirstChar(field.getName())));
            queryBuilder.append("}\n");
        }
        return queryBuilder.toString();
    }

    private String upperFirstChar(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }



    @Override
    public String getPackageName() {
        return packageConfig.getRepositoryPackage();
    }

    @Override
    public String getClassName(PojoInfo pojoInfo) {
        return pojoInfo.getClassName() + "Repository";
    }
}