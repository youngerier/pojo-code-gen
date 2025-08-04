package com.example.generator.generators;

import com.abc.entity.web.support.Pagination;
import com.example.generator.CodeGenerator;
import com.example.generator.model.PackageConfig;
import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service实现类生成器
 */
@Slf4j
public class ServiceImplGenerator implements CodeGenerator {


    private final PackageConfig packageConfig;

    public ServiceImplGenerator(PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
    }
    @Override
    public TypeSpec generate(PojoInfo pojoInfo) {
        // 获取实体类名
        String entityName = pojoInfo.getClassName();
        // 创建实体类类型
        ClassName entityType = ClassName.get(pojoInfo.getPackageName(), entityName);
        // 创建DTO类型
        ClassName dtoType = ClassName.get(packageConfig.getDtoPackage(), packageConfig.getDtoClassName(entityName));
        // 创建Service接口类型
        ClassName serviceType = ClassName.get(
                packageConfig.getServicePackage(),
                packageConfig.getServiceClassName(entityName));
        // 创建Repository接口类型
        ClassName repositoryType = ClassName.get(
                packageConfig.getRepositoryPackage(),
                packageConfig.getRepositoryClassName(entityName));
        // 创建Mapstruct Mapper类型
        ClassName mapperType = ClassName.get(
                packageConfig.getConvertorPackage(),
                packageConfig.getConvertorClassName(entityName));

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

        // 添加Mapper字段
        String mapperFieldName = lowerFirstChar(entityName) + "Convertor";
        FieldSpec mapperField = FieldSpec.builder(mapperType, mapperFieldName)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();
        classBuilder.addField(mapperField);

        // 添加构造函数
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(repositoryType, repositoryFieldName)
                .addParameter(mapperType, mapperFieldName)
                .addStatement("this.$N = $N", repositoryFieldName, repositoryFieldName)
                .addStatement("this.$N = $N", mapperFieldName, mapperFieldName)
                .build();
        classBuilder.addMethod(constructor);

        // 添加createXxx方法
        MethodSpec createMethod = MethodSpec.methodBuilder("create" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(dtoType, lowerFirstChar(entityName) + "DTO")
                .returns(dtoType)
                .addStatement("$T entity = $N.toEntity($N)", entityType, mapperFieldName, lowerFirstChar(entityName) + "DTO")
                .addStatement("$N.insert(entity)", repositoryFieldName)
                .addStatement("return $N.toDto(entity)", mapperFieldName)
                .build();
        classBuilder.addMethod(createMethod);

        // 添加getXxxById方法
        MethodSpec getByIdMethod = MethodSpec.methodBuilder("get" + entityName + "ById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeName.LONG, "id")
                .returns(dtoType)
                .addStatement("$T entity = $N.selectOneById(id)", entityType, repositoryFieldName)
                .addStatement("return $N.toDto(entity)", mapperFieldName)
                .build();
        classBuilder.addMethod(getByIdMethod);

        

        // 添加queryXxxs方法
        ParameterizedTypeName listOfDto = ParameterizedTypeName.get(
                ClassName.get(List.class), dtoType);
        MethodSpec queryMethod = MethodSpec.methodBuilder("query" + entityName + "s")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ClassName.get(packageConfig.getRequestPackage(), pojoInfo.getClassName() + "Query"), "query")
                .returns(listOfDto)
                .addStatement("return $N.selectListByQuery(query).stream().map($N::toDto).collect($T.toList())", repositoryFieldName, mapperFieldName, Collectors.class)
                .build();
        classBuilder.addMethod(queryMethod);

        // 添加pageQueryXxxs方法
        MethodSpec pageQueryMethod = MethodSpec.methodBuilder("pageQuery" + entityName + "s")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ClassName.get(packageConfig.getRequestPackage(), pojoInfo.getClassName() + "Query"), "query")
                .returns(ParameterizedTypeName.get(ClassName.get(Pagination.class), dtoType))
                .addStatement("return $N.page(query).map($N::toDto)", repositoryFieldName, mapperFieldName)
                .build();
        classBuilder.addMethod(pageQueryMethod);

        // 添加updateXxx方法
        MethodSpec updateMethod = MethodSpec.methodBuilder("update" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeName.LONG, "id")
                .addParameter(dtoType, lowerFirstChar(entityName) + "DTO")
                .returns(dtoType)
                .addStatement("$T existingEntity = $N.selectOneById(id)", entityType, repositoryFieldName)
                .beginControlFlow("if (existingEntity == null)")
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$T updatedEntity = $N.toEntity($N)", entityType, mapperFieldName, lowerFirstChar(entityName) + "DTO")
                .addStatement("updatedEntity.setId(id)") // Ensure ID is set for update
                .addStatement("$N.update(updatedEntity)", repositoryFieldName)
                .addStatement("return $N.toDto(updatedEntity)", mapperFieldName)
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
    public String getPackageName() {
        return packageConfig.getServiceImplPackage();
    }

    @Override
    public String getClassName(PojoInfo pojoInfo) {
        return packageConfig.getServiceImplClassName(pojoInfo.getClassName());
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