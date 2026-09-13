package io.github.youngerier.generator.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.youngerier.support.page.Pagination;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.GeneratedType;
import io.github.youngerier.generator.model.PackageStructure;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Service 接口生成器
 */
public class ServiceGenerator extends BaseGenerator {

    public ServiceGenerator(PackageStructure packageStructure) {
        super(packageStructure, GeneratedType.SERVICE);
    }

    @Override
    public TypeSpec generate(ClassMetadata metadata) {
        String entityName = metadata.getClassName();
        String dtoParameter = metadata.getCamelClassName() + "DTO";
        ClassName dtoType = packages.dto();

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(getClassName())
                .addModifiers(Modifier.PUBLIC);

        builder.addMethod(MethodSpec.methodBuilder("create" + entityName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dtoType)
                .addParameter(dtoType, dtoParameter)
                .addJavadoc("创建$L\n", entityName)
                .addJavadoc("@param $L $L数据传输对象\n", dtoParameter, entityName)
                .addJavadoc("@return 创建的$L对象\n", entityName)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("get" + entityName + "ById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dtoType)
                .addParameter(TypeName.LONG, "id")
                .addJavadoc("根据ID查询$L\n", entityName)
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 对应的$L对象\n", entityName)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("query" + entityName + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), dtoType))
                .addParameter(packages.query(), "query")
                .addJavadoc("查询所有$L\n", entityName)
                .addJavadoc("@return $L对象列表\n", entityName)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("pageQuery" + entityName + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(Pagination.class), dtoType))
                .addParameter(packages.query(), "query")
                .addJavadoc("分页查询$L\n", entityName)
                .addJavadoc("@return $L对象列表\n", entityName)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("update" + entityName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dtoType)
                .addParameter(TypeName.LONG, "id")
                .addParameter(dtoType, dtoParameter)
                .addJavadoc("更新$L\n", entityName)
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@param $L $L数据传输对象\n", dtoParameter, entityName)
                .addJavadoc("@return 更新后的$L对象\n", entityName)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("delete" + entityName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.BOOLEAN)
                .addParameter(TypeName.LONG, "id")
                .addJavadoc("删除$L\n", entityName)
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 是否删除成功\n")
                .build());

        Javadocs.appendClassComment(builder, metadata, "服务接口");
        return builder.build();
    }
}
