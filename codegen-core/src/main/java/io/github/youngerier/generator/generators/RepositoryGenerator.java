package io.github.youngerier.generator.generators;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.squareup.javapoet.ClassName;
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
                .addStatement("$T $L = $T.$L", tableRefs, tableVarName, tableRefs, staticTableField)
                .addStatement("$T wrapper = $T.withOrder(query).from($L)",
                        QueryWrapper.class, QueryWrapperHelper.class, tableVarName);

        // 等值条件按字段逐一判空后拼接：未传的查询条件绝不能以 = null 进入 SQL
        for (ClassMetadata.FieldInfo field : metadata.getFields()) {
            String getter = getterName(field.getName());
            method.beginControlFlow("if (query.$L() != null)", getter)
                    .addStatement("wrapper.and($L.$L.eq(query.$L()))",
                            tableVarName, field.getName(), getter)
                    .endControlFlow();
        }

        // 时间范围条件只在实体存在对应审计字段时生成
        if (hasField(metadata, "gmtCreate")) {
            method.addStatement("wrapper.and($L.gmtCreate.ge(query.getMinGmtCreate()))", tableVarName);
            method.addStatement("wrapper.and($L.gmtCreate.le(query.getMaxGmtCreate()))", tableVarName);
        }
        if (hasField(metadata, "gmtModified")) {
            method.addStatement("wrapper.and($L.gmtModified.ge(query.getMinGmtModified()))", tableVarName);
            method.addStatement("wrapper.and($L.gmtModified.le(query.getMaxGmtModified()))", tableVarName);
        }

        method.addStatement("return wrapper");
        return method.build();
    }

    private static String getterName(String fieldName) {
        return "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    private static boolean hasField(ClassMetadata metadata, String fieldName) {
        return metadata.getFields().stream().anyMatch(field -> fieldName.equals(field.getName()));
    }

    private ClassName entityType(ClassMetadata metadata) {
        return ClassName.get(metadata.getPackageName(), metadata.getClassName());
    }
}
