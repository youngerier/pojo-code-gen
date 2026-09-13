package io.github.youngerier.generator.generators;

import com.mybatisflex.core.paginate.Page;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
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
import java.util.stream.Collectors;

/**
 * Service 实现类生成器
 */
public class ServiceImplGenerator extends BaseGenerator {

    private static final ClassName SPRING_SERVICE =
            ClassName.get("org.springframework.stereotype", "Service");

    public ServiceImplGenerator(PackageStructure packageLayout) {
        super(packageLayout, GeneratedType.SERVICE_IMPL);
    }

    @Override
    public TypeSpec generate(ClassMetadata metadata) {
        String entityName = metadata.getClassName();
        String camelName = metadata.getCamelClassName();
        String dtoParameter = camelName + "DTO";
        String repositoryField = camelName + "Repository";
        String convertorField = camelName + "Convertor";

        ClassName entityType = ClassName.get(metadata.getPackageName(), entityName);
        ClassName dtoType = packages.dto();

        TypeSpec.Builder builder = TypeSpec.classBuilder(getClassName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(SPRING_SERVICE)
                .addSuperinterface(packages.service());

        builder.addField(FieldSpec.builder(packages.repository(), repositoryField)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build());
        builder.addField(FieldSpec.builder(packages.convertor(), convertorField)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("$T.INSTANCE", packages.convertor())
                .build());

        builder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(packages.repository(), repositoryField)
                .addStatement("this.$N = $N", repositoryField, repositoryField)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("create" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(dtoType, dtoParameter)
                .returns(dtoType)
                .addStatement("$T entity = $N.toEntity($N)", entityType, convertorField, dtoParameter)
                .addStatement("$N.save(entity)", repositoryField)
                .addStatement("return $N.toDto(entity)", convertorField)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("get" + entityName + "ById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeName.LONG, "id")
                .returns(dtoType)
                .addStatement("$T entity = $N.getById(id)", entityType, repositoryField)
                .addStatement("return $N.toDto(entity)", convertorField)
                .build());

        ParameterizedTypeName listOfDto = ParameterizedTypeName.get(ClassName.get(List.class), dtoType);
        builder.addMethod(MethodSpec.methodBuilder("query" + entityName + "s")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(packages.query(), "query")
                .returns(listOfDto)
                .addStatement("return $N.selectListByQuery(query).stream().map($N::toDto).collect($T.toList())",
                        repositoryField, convertorField, Collectors.class)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("pageQuery" + entityName + "s")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(packages.query(), "query")
                .returns(ParameterizedTypeName.get(ClassName.get(Pagination.class), dtoType))
                .addStatement("$T<$T> page = $N.page(query).map($N::toDto)",
                        Page.class, dtoType, repositoryField, convertorField)
                .addStatement("return $T.of(page.getRecords(), query, page.getTotalRow())", Pagination.class)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("update" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeName.LONG, "id")
                .addParameter(dtoType, dtoParameter)
                .returns(dtoType)
                .addStatement("$T existingEntity = $N.getById(id)", entityType, repositoryField)
                .beginControlFlow("if (existingEntity == null)")
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$T updatedEntity = $N.toEntity($N)", entityType, convertorField, dtoParameter)
                .addStatement("updatedEntity.setId(id)")
                .addStatement("$N.updateById(updatedEntity)", repositoryField)
                .addStatement("return $N.toDto(updatedEntity)", convertorField)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("delete" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeName.LONG, "id")
                .returns(TypeName.BOOLEAN)
                .addStatement("return $N.removeById(id)", repositoryField)
                .build());

        Javadocs.appendClassComment(builder, metadata, "服务实现类");
        return builder.build();
    }
}
