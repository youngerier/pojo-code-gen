package io.github.youngerier.generator.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.GeneratedType;
import io.github.youngerier.generator.model.PackageStructure;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * MapStruct 转换器生成器
 */
public class MapstructGenerator extends BaseGenerator {

    private static final ClassName MAPPER_ANNOTATION = ClassName.get("org.mapstruct", "Mapper");
    private static final ClassName MAPSTRUCT_MAPPERS = ClassName.get("org.mapstruct.factory", "Mappers");

    public MapstructGenerator(PackageStructure packageLayout) {
        super(packageLayout, GeneratedType.CONVERTOR);
    }

    @Override
    public TypeSpec generate(ClassMetadata metadata) {
        ClassName entityType = ClassName.get(metadata.getPackageName(), metadata.getClassName());
        ClassName dtoType = packages.dto();
        ClassName requestType = packages.request();
        ClassName responseType = packages.response();

        ParameterizedTypeName listOfEntity = ParameterizedTypeName.get(ClassName.get(List.class), entityType);
        ParameterizedTypeName listOfDto = ParameterizedTypeName.get(ClassName.get(List.class), dtoType);
        ParameterizedTypeName listOfResponse = ParameterizedTypeName.get(ClassName.get(List.class), responseType);

        String entity = metadata.getCamelClassName();

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(getClassName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(MAPPER_ANNOTATION);

        // INSTANCE 常量
        builder.addField(FieldSpec.builder(packages.convertor(), "INSTANCE")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.getMapper($T.class)", MAPSTRUCT_MAPPERS, packages.convertor())
                .build());

        builder.addMethod(convertMethod("toDto", entityType, entity, dtoType));
        builder.addMethod(convertMethod("toDto", requestType, entity + "Request", dtoType));
        builder.addMethod(convertMethod("toEntity", dtoType, entity + "DTO", entityType));
        builder.addMethod(convertMethod("toEntity", requestType, entity + "Request", entityType));
        builder.addMethod(convertMethod("toResponse", entityType, entity, responseType));
        builder.addMethod(convertMethod("toResponse", dtoType, entity, responseType));
        builder.addMethod(convertMethod("toDtoList", listOfEntity, entity + "List", listOfDto));
        builder.addMethod(convertMethod("toResponseList", listOfEntity, entity + "List", listOfResponse));

        Javadocs.appendClassComment(builder, metadata, "对象转换器");
        return builder.build();
    }

    private MethodSpec convertMethod(String name, TypeName parameterType,
                                     String parameterName, TypeName returnType) {
        return MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(parameterType, parameterName)
                .returns(returnType)
                .build();
    }
}
