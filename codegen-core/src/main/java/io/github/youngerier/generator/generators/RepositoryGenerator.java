package io.github.youngerier.generator.generators;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.youngerier.support.page.flex.QueryWrapperHelper;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.GeneratedType;
import io.github.youngerier.generator.model.PackageStructure;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Repository 实现类生成器 - 基于 MyBatis Flex ServiceImpl
 */
public class RepositoryGenerator extends BaseGenerator {

    private static final ClassName FLEX_SERVICE_IMPL =
            ClassName.get("com.mybatisflex.spring.service.impl", "ServiceImpl");
    private static final ClassName FLEX_ISERVICE =
            ClassName.get("com.mybatisflex.core.service", "IService");

    public RepositoryGenerator(PackageStructure packageLayout) {
        super(packageLayout, GeneratedType.REPOSITORY);
    }

    @Override
    public TypeSpec generate(ClassMetadata metadata) {
        ClassName entityType = entityType(metadata);

        TypeSpec.Builder builder = TypeSpec.classBuilder(getClassName())
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(FLEX_SERVICE_IMPL, packages.mapper(), entityType))
                .addSuperinterface(ParameterizedTypeName.get(FLEX_ISERVICE, entityType));

        Javadocs.appendClassComment(builder, metadata, "数据访问层实现类");

        builder.addMethod(buildQueryWrapperMethod(metadata));
        builder.addMethod(buildSelectListByQueryMethod(metadata));
        builder.addMethod(buildPageMethod(metadata));
        return builder.build();
    }

    private MethodSpec buildSelectListByQueryMethod(ClassMetadata metadata) {
        return MethodSpec.methodBuilder("selectListByQuery")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(packages.query(), "query")
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), entityType(metadata)))
                .addStatement("return getMapper().selectListByQuery(buildQueryWrapper(query))")
                .build();
    }

    private MethodSpec buildPageMethod(ClassMetadata metadata) {
        ClassName entityType = entityType(metadata);
        return MethodSpec.methodBuilder("page")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(packages.query(), "query")
                .returns(ParameterizedTypeName.get(ClassName.get(Page.class), entityType))
                .addStatement("$T<$T> page = new $T<>(query.getQueryPage(), query.getQuerySize())",
                        ClassName.get(Page.class), entityType, ClassName.get(Page.class))
                .addStatement("return getMapper().paginate(page, buildQueryWrapper(query))")
                .build();
    }

    private MethodSpec buildQueryWrapperMethod(ClassMetadata metadata) {
        ClassName tableRefs = ClassName.get(
                metadata.getPackageName() + ".table", metadata.getClassName() + "TableRefs");
        String tableVarName = metadata.getCamelClassName() + "TableRefs";
        String staticTableField = metadata.getCamelClassName();

        MethodSpec.Builder method = MethodSpec.methodBuilder("buildQueryWrapper")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(packages.query(), "query")
                .returns(QueryWrapper.class)
                .addStatement("$T $L = $T.$L", tableRefs, tableVarName, tableRefs, staticTableField);

        CodeBlock.Builder queryWrapper = CodeBlock.builder()
                .add("return $T.withOrder(query)\n", QueryWrapperHelper.class)
                .indent()
                .add(".from($L)\n", tableVarName);

        boolean firstField = true;
        for (ClassMetadata.FieldInfo field : metadata.getFields()) {
            String getter = "get" + Character.toUpperCase(field.getName().charAt(0))
                    + field.getName().substring(1);
            String connector = firstField ? ".where" : ".and";
            firstField = false;
            queryWrapper.add("$L($L.$L.eq(query.$L()))\n", connector, tableVarName, field.getName(), getter);
        }

        queryWrapper.add(".and($L.gmtCreate.ge(query.getMinGmtCreate()))\n", tableVarName);
        queryWrapper.add(".and($L.gmtCreate.le(query.getMaxGmtCreate()))\n", tableVarName);
        queryWrapper.add(".and($L.gmtModified.ge(query.getMinGmtModified()))\n", tableVarName);
        queryWrapper.add(".and($L.gmtModified.le(query.getMaxGmtModified()));\n", tableVarName);
        queryWrapper.unindent();

        return method.addCode(queryWrapper.build()).build();
    }

    private ClassName entityType(ClassMetadata metadata) {
        return ClassName.get(metadata.getPackageName(), metadata.getClassName());
    }
}
