package com.example.generator.generators;

import com.example.generator.CodeGenerator;
import com.example.generator.model.PackageStructure;
import com.example.generator.model.ClassMetadata;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * MapStruct转换器生成器
 */
@Slf4j
public class MapstructGenerator implements CodeGenerator {

    private final PackageStructure packageLayout;

    public MapstructGenerator(PackageStructure packageLayout) {
        this.packageLayout = packageLayout;
    }

    @Override
    public TypeSpec generate(ClassMetadata pojoInfo) {
        // 获取实体类名
        String entityName = pojoInfo.getClassName();
        // 创建实体类类型
        ClassName entityType = ClassName.get(pojoInfo.getPackageName(), entityName);
        // 创建DTO类型
        ClassName dtoType = ClassName.get(packageLayout.getDtoPackage(), packageLayout.getDtoClassName(entityName));
        // 创建Request类型
        ClassName requestType = ClassName.get(packageLayout.getRequestPackage(), packageLayout.getRequestClassName(entityName));
        // 创建Response类型
        ClassName responseType = ClassName.get(packageLayout.getResponsePackage(), packageLayout.getResponseClassName(entityName));

        // 创建List<Entity>类型
        ParameterizedTypeName listOfEntity = ParameterizedTypeName.get(
                ClassName.get(List.class), entityType);
        // 创建List<DTO>类型
        ParameterizedTypeName listOfDto = ParameterizedTypeName.get(
                ClassName.get(List.class), dtoType);
        // 创建List<Response>类型
        ParameterizedTypeName listOfResponse = ParameterizedTypeName.get(
                ClassName.get(List.class), responseType);

        // 创建接口构建器
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.mapstruct", "Mapper"));

        // 添加INSTANCE常量
        ClassName convertorType = ClassName.get(packageLayout.getConvertorPackage(), getClassName(pojoInfo));
        FieldSpec instanceField = FieldSpec.builder(convertorType, "INSTANCE")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.getMapper($T.class)", 
                    ClassName.get("org.mapstruct.factory", "Mappers"), convertorType)
                .build();
        interfaceBuilder.addField(instanceField);

        // 添加实体到DTO的转换方法
        MethodSpec entityToDtoMethod = MethodSpec.methodBuilder("toDto")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(entityType, lowerFirstChar(entityName))
                .returns(dtoType)
                .build();
        interfaceBuilder.addMethod(entityToDtoMethod);

        // 添加DTO到实体的转换方法
        MethodSpec dtoToEntityMethod = MethodSpec.methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(dtoType, lowerFirstChar(entityName) + "DTO")
                .returns(entityType)
                .build();
        interfaceBuilder.addMethod(dtoToEntityMethod);

        // 添加Request到实体的转换方法
        MethodSpec requestToEntityMethod = MethodSpec.methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(requestType, lowerFirstChar(entityName) + "Request")
                .returns(entityType)
                .build();
        interfaceBuilder.addMethod(requestToEntityMethod);

        // 添加实体到Response的转换方法
        MethodSpec entityToResponseMethod = MethodSpec.methodBuilder("toResponse")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(entityType, lowerFirstChar(entityName))
                .returns(responseType)
                .build();
        interfaceBuilder.addMethod(entityToResponseMethod);

        // 添加实体列表到DTO列表的转换方法
        MethodSpec entityListToDtoListMethod = MethodSpec.methodBuilder("toDtoList")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(listOfEntity, lowerFirstChar(entityName) + "List")
                .returns(listOfDto)
                .build();
        interfaceBuilder.addMethod(entityListToDtoListMethod);

        // 添加实体列表到Response列表的转换方法
        MethodSpec entityListToResponseListMethod = MethodSpec.methodBuilder("toResponseList")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(listOfEntity, lowerFirstChar(entityName) + "List")
                .returns(listOfResponse)
                .build();
        interfaceBuilder.addMethod(entityListToResponseListMethod);

        // 添加接口注释
        if (pojoInfo.getClassComment() != null && !pojoInfo.getClassComment().isEmpty()) {
            interfaceBuilder.addJavadoc(pojoInfo.getClassComment() + "\n");
            interfaceBuilder.addJavadoc("对象转换器\n");
        }

        return interfaceBuilder.build();
    }

    @Override
    public String getPackageName() {
        return packageLayout.getConvertorPackage();
    }

    @Override
    public String getClassName(ClassMetadata pojoInfo) {
        return packageLayout.getConvertorClassName(pojoInfo.getClassName());
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