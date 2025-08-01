package com.example.generator.generators;

import com.example.generator.CodeGenerator;
import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Service实现类生成器
 */
public class ServiceImplGenerator implements CodeGenerator {

    @Override
    public TypeSpec generate(PojoInfo pojoInfo) {
        // 获取实体类名
        String entityName = pojoInfo.getClassName();
        // 创建实体类类型
        ClassName entityType = ClassName.get(pojoInfo.getPackageName(), entityName);
        // 创建Service接口类型
        ClassName serviceType = ClassName.get(
                pojoInfo.getPackageName().replace(".entity", ".service"),
                entityName + "Service");
        // 创建Repository接口类型
        ClassName repositoryType = ClassName.get(
                pojoInfo.getPackageName().replace(".entity", ".repository"),
                entityName + "Repository");

        // 创建类构建器
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Service"))
                .addSuperinterface(serviceType);

        // 添加Repository字段
        String repositoryFieldName = lowerFirstChar(entityName) + "Repository";
        FieldSpec repositoryField = FieldSpec.builder(repositoryType, repositoryFieldName)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();
        classBuilder.addField(repositoryField);

        // 添加构造函数
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(repositoryType, repositoryFieldName)
                .addStatement("this.$N = $N", repositoryFieldName, repositoryFieldName)
                .build();
        classBuilder.addMethod(constructor);

        // 添加createXxx方法
        MethodSpec createMethod = MethodSpec.methodBuilder("create" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(entityType, lowerFirstChar(entityName))
                .returns(entityType)
                .addStatement("$N.insert($N)", repositoryFieldName, lowerFirstChar(entityName))
                .addStatement("return $N", lowerFirstChar(entityName))
                .build();
        classBuilder.addMethod(createMethod);

        // 添加getXxxById方法
        MethodSpec getByIdMethod = MethodSpec.methodBuilder("get" + entityName + "ById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(long.class, "id")
                .returns(entityType)
                .addStatement("return $N.selectOneById(id)", repositoryFieldName)
                .build();
        classBuilder.addMethod(getByIdMethod);

        // 添加getAllXxx方法
        ParameterizedTypeName listOfEntity = ParameterizedTypeName.get(
                ClassName.get(List.class), entityType);
        MethodSpec getAllMethod = MethodSpec.methodBuilder("getAll" + entityName + "s")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(listOfEntity)
                .addStatement("return $N.selectAll()", repositoryFieldName)
                .build();
        classBuilder.addMethod(getAllMethod);

        // 添加updateXxx方法
        MethodSpec updateMethod = MethodSpec.methodBuilder("update" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(entityType, lowerFirstChar(entityName))
                .returns(entityType)
                .addStatement("$N.update($N)", repositoryFieldName, lowerFirstChar(entityName))
                .addStatement("return $N", lowerFirstChar(entityName))
                .build();
        classBuilder.addMethod(updateMethod);

        // 添加deleteXxx方法
        MethodSpec deleteMethod = MethodSpec.methodBuilder("delete" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(long.class, "id")
                .returns(TypeName.BOOLEAN)
                .addStatement("return $N.deleteById(id) > 0", repositoryFieldName)
                .build();
        classBuilder.addMethod(deleteMethod);

        // 添加类注释
        if (pojoInfo.getClassComment() != null && !pojoInfo.getClassComment().isEmpty()) {
            classBuilder.addJavadoc(pojoInfo.getClassComment() + "\n");
            classBuilder.addJavadoc("服务实现类\n");
        }

        return classBuilder.build();
    }

    @Override
    public String getPackageName(PojoInfo pojoInfo) {
        return pojoInfo.getPackageName().replace(".entity", ".service.impl");
    }

    @Override
    public String getClassName(PojoInfo pojoInfo) {
        return pojoInfo.getClassName() + "ServiceImpl";
    }

    /**
     * 将字符串的首字母转为小写
     */
    private String lowerFirstChar(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}