package com.example.generator.generators;

import com.abc.web.support.Pagination;
import com.example.generator.CodeGenerator;
import com.example.generator.model.PackageConfig;
import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Service接口生成器
 */
@Slf4j
public class ServiceGenerator implements CodeGenerator {
    private final PackageConfig packageConfig;

    public ServiceGenerator(PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
    }

    @Override
    public TypeSpec generate(PojoInfo pojoInfo) {
        String dtoClassName = packageConfig.getDtoClassName(pojoInfo.getClassName());
        ClassName dtoType = ClassName.get(packageConfig.getDtoPackage(), dtoClassName);
        ClassName listType = ClassName.get(List.class);
        ParameterizedTypeName listOfDto = ParameterizedTypeName.get(listType, dtoType);

        // 创建接口构建器
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC);

        // 添加接口注释
        if (pojoInfo.getClassComment() != null && !pojoInfo.getClassComment().isEmpty()) {
            interfaceBuilder.addJavadoc(pojoInfo.getClassComment() + "\n");
            interfaceBuilder.addJavadoc("服务接口\n");
        }

        // 添加创建方法
        MethodSpec createMethod = MethodSpec.methodBuilder("create" + pojoInfo.getClassName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dtoType)
                .addParameter(dtoType, pojoInfo.getClassName().toLowerCase() + "DTO")
                .addJavadoc("创建$L\n", pojoInfo.getClassName())
                .addJavadoc("@param $L $L数据传输对象\n",
                        pojoInfo.getClassName().toLowerCase() + "DTO",
                        pojoInfo.getClassName())
                .addJavadoc("@return 创建的$L对象\n", pojoInfo.getClassName())
                .build();
        interfaceBuilder.addMethod(createMethod);

        // 添加查询单个对象方法
        MethodSpec getByIdMethod = MethodSpec.methodBuilder("get" + pojoInfo.getClassName() + "ById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dtoType)
                .addParameter(TypeName.LONG, "id")
                .addJavadoc("根据ID查询$L\n", pojoInfo.getClassName())
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 对应的$L对象\n", pojoInfo.getClassName())
                .build();
        interfaceBuilder.addMethod(getByIdMethod);

        // 添加查询所有对象方法
        MethodSpec getAllMethod = MethodSpec.methodBuilder("query" + pojoInfo.getClassName() + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(listOfDto)
                .addParameter(ClassName.get(packageConfig.getRequestPackage(), packageConfig.getQueryClassName(pojoInfo.getClassName())), "query")
                .addJavadoc("查询所有$L\n", pojoInfo.getClassName())
                .addJavadoc("@return $L对象列表\n", pojoInfo.getClassName())
                .build();
        interfaceBuilder.addMethod(getAllMethod);

        // 添加分页查询方法
        MethodSpec pageQueryMethod = MethodSpec.methodBuilder("pageQuery" + pojoInfo.getClassName() + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(Pagination.class), dtoType))
                .addParameter(ClassName.get(packageConfig.getRequestPackage(), packageConfig.getQueryClassName(pojoInfo.getClassName())), "query")
                .addJavadoc("分页查询$L\n", pojoInfo.getClassName())
                .addJavadoc("@return $L对象列表\n", pojoInfo.getClassName())
                .build();
        interfaceBuilder.addMethod(pageQueryMethod);
 
         // 添加更新方法
        MethodSpec updateMethod = MethodSpec.methodBuilder("update" + pojoInfo.getClassName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dtoType)
                .addParameter(TypeName.LONG, "id")
                .addParameter(dtoType, pojoInfo.getClassName().toLowerCase() + "DTO")
                .addJavadoc("更新$L\n", pojoInfo.getClassName())
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@param $L $L数据传输对象\n",
                        pojoInfo.getClassName().toLowerCase() + "DTO",
                        pojoInfo.getClassName())
                .addJavadoc("@return 更新后的$L对象\n", pojoInfo.getClassName())
                .build();
        interfaceBuilder.addMethod(updateMethod);

        // 添加删除方法
        MethodSpec deleteMethod = MethodSpec.methodBuilder("delete" + pojoInfo.getClassName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.BOOLEAN)
                .addParameter(TypeName.LONG, "id")
                .addJavadoc("删除$L\n", pojoInfo.getClassName())
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 是否删除成功\n")
                .build();
        interfaceBuilder.addMethod(deleteMethod);

        return interfaceBuilder.build();
    }

    @Override
    public String getPackageName() {
        return packageConfig.getServicePackage();
    }

    @Override
    public String getClassName(PojoInfo pojoInfo) {
        return packageConfig.getServiceClassName(pojoInfo.getClassName());
    }
}
