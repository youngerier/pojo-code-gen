package io.github.youngerier.generator.generators;

import io.github.youngerier.support.Pagination;
import io.github.youngerier.generator.CodeGenerator;
import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
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
    private final PackageStructure packageStructure;

    public ServiceGenerator(PackageStructure packageStructure) {
        this.packageStructure = packageStructure;
    }

    @Override
    public TypeSpec generate(ClassMetadata classMetadata) {
        String dtoClassName = packageStructure.getDtoClassName(classMetadata.getClassName());
        ClassName dtoType = ClassName.get(packageStructure.getDtoPackage(), dtoClassName);
        ClassName listType = ClassName.get(List.class);
        ParameterizedTypeName listOfDto = ParameterizedTypeName.get(listType, dtoType);

        // 创建接口构建器
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(getClassName(classMetadata))
                .addModifiers(Modifier.PUBLIC);

        // 添加接口注释
        if (classMetadata.getClassComment() != null && !classMetadata.getClassComment().isEmpty()) {
            interfaceBuilder.addJavadoc(classMetadata.getClassComment() + "\n");
            interfaceBuilder.addJavadoc("服务接口\n");
        }

        // 添加创建方法
        MethodSpec createMethod = MethodSpec.methodBuilder("create" + classMetadata.getClassName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dtoType)
                .addParameter(dtoType, classMetadata.getCamelClassName() + "DTO")
                .addJavadoc("创建$L\n", classMetadata.getClassName())
                .addJavadoc("@param $L $L数据传输对象\n",
                        classMetadata.getCamelClassName() + "DTO",
                        classMetadata.getClassName())
                .addJavadoc("@return 创建的$L对象\n", classMetadata.getClassName())
                .build();
        interfaceBuilder.addMethod(createMethod);

        // 添加查询单个对象方法
        MethodSpec getByIdMethod = MethodSpec.methodBuilder("get" + classMetadata.getClassName() + "ById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dtoType)
                .addParameter(TypeName.LONG, "id")
                .addJavadoc("根据ID查询$L\n", classMetadata.getClassName())
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 对应的$L对象\n", classMetadata.getClassName())
                .build();
        interfaceBuilder.addMethod(getByIdMethod);

        // 添加查询所有对象方法
        MethodSpec getAllMethod = MethodSpec.methodBuilder("query" + classMetadata.getClassName() + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(listOfDto)
                .addParameter(ClassName.get(packageStructure.getRequestPackage(), packageStructure.getQueryClassName(classMetadata.getClassName())), "query")
                .addJavadoc("查询所有$L\n", classMetadata.getClassName())
                .addJavadoc("@return $L对象列表\n", classMetadata.getClassName())
                .build();
        interfaceBuilder.addMethod(getAllMethod);

        // 添加分页查询方法
        MethodSpec pageQueryMethod = MethodSpec.methodBuilder("pageQuery" + classMetadata.getClassName() + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(Pagination.class), dtoType))
                .addParameter(ClassName.get(packageStructure.getRequestPackage(), packageStructure.getQueryClassName(classMetadata.getClassName())), "query")
                .addJavadoc("分页查询$L\n", classMetadata.getClassName())
                .addJavadoc("@return $L对象列表\n", classMetadata.getClassName())
                .build();
        interfaceBuilder.addMethod(pageQueryMethod);
 
         // 添加更新方法
        MethodSpec updateMethod = MethodSpec.methodBuilder("update" + classMetadata.getClassName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dtoType)
                .addParameter(TypeName.LONG, "id")
                .addParameter(dtoType, classMetadata.getCamelClassName() + "DTO")
                .addJavadoc("更新$L\n", classMetadata.getClassName())
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@param $L $L数据传输对象\n",
                        classMetadata.getCamelClassName() + "DTO",
                        classMetadata.getClassName())
                .addJavadoc("@return 更新后的$L对象\n", classMetadata.getClassName())
                .build();
        interfaceBuilder.addMethod(updateMethod);

        // 添加删除方法
        MethodSpec deleteMethod = MethodSpec.methodBuilder("delete" + classMetadata.getClassName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.BOOLEAN)
                .addParameter(TypeName.LONG, "id")
                .addJavadoc("删除$L\n", classMetadata.getClassName())
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 是否删除成功\n")
                .build();
        interfaceBuilder.addMethod(deleteMethod);

        return interfaceBuilder.build();
    }

    @Override
    public String getPackageName() {
        return packageStructure.getServicePackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getServiceClassName(classMetadata.getClassName());
    }
}
