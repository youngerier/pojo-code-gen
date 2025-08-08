package com.example.generator.generators;

import com.example.generator.CodeGenerator;
import com.example.generator.model.PackageLayout;
import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Repository接口生成器 - 基于MyBatis Flex
 */
@Slf4j
public class RepositoryGenerator implements CodeGenerator {
    private final PackageLayout packageLayout;

    public RepositoryGenerator(PackageLayout packageLayout) {
        this.packageLayout = packageLayout;
    }

    @Override
    public TypeSpec generate(PojoInfo pojoInfo) {
        String entityName = pojoInfo.getClassName();
        ClassName entityType = ClassName.get(pojoInfo.getPackageName(), entityName);
        ClassName baseMapperType = ClassName.get("com.mybatisflex.core", "BaseMapper");
        ParameterizedTypeName baseMapperOfEntity = ParameterizedTypeName.get(baseMapperType, entityType);

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(baseMapperOfEntity);

        if (pojoInfo.getClassComment() != null && !pojoInfo.getClassComment().isEmpty()) {
            interfaceBuilder.addJavadoc(pojoInfo.getClassComment() + "\n");
            interfaceBuilder.addJavadoc("数据访问层接口\n");
        }

        ClassName tableRefs = ClassName.get(pojoInfo.getPackageName() + ".table", pojoInfo.getClassName() + "TableRefs");
        String tableVarName = pojoInfo.getClassName().toLowerCase() + "TableRefs";
        String staticTableFieldName = pojoInfo.getClassName().toLowerCase();

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("selectListByQuery")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(ClassName.get(packageLayout.getRequestPackage(), pojoInfo.getClassName() + "Query"), "query")
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), entityType));

        methodBuilder.addStatement("$T $L = $T.$L", tableRefs, tableVarName, tableRefs, staticTableFieldName);

        CodeBlock.Builder queryBuilder = CodeBlock.builder();
        queryBuilder.add("$T queryWrapper = $T.create()\n", QueryWrapper.class, QueryWrapper.class);
        queryBuilder.indent();
        queryBuilder.add(".from($L)\n", tableVarName);

        for (PojoInfo.FieldInfo field : pojoInfo.getFields()) {
            queryBuilder.add(".where($L.$L.$L.eq(query.get$L()))\n",
                    tableVarName, staticTableFieldName, field.getName(), upperFirstChar(field.getName()));
        }

        addTimeRangeConditions(queryBuilder, tableVarName, staticTableFieldName);
        
        String idField = pojoInfo.getFields().stream()
            .map(PojoInfo.FieldInfo::getName)
            .filter("id"::equalsIgnoreCase)
            .findFirst()
            .orElse("id").toLowerCase();

        queryBuilder.add(".orderBy($L.$L.$L.desc());\n", tableVarName, staticTableFieldName, idField);
        queryBuilder.unindent();

        methodBuilder.addCode(queryBuilder.build());
        methodBuilder.addStatement("return selectListByQuery(queryWrapper)");
        interfaceBuilder.addMethod(methodBuilder.build());

        MethodSpec.Builder pageMethodBuilder = MethodSpec.methodBuilder("page")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(ClassName.get(packageLayout.getRequestPackage(), pojoInfo.getClassName() + "Query"), "query")
                .returns(ParameterizedTypeName.get(ClassName.get(Page.class), entityType));

        pageMethodBuilder.addStatement("$T<$T> page = new Page<>(query.getQueryPage(), query.getQuerySize())", Page.class, entityType);
        pageMethodBuilder.addStatement("$T $L = $T.$L", tableRefs, tableVarName, tableRefs, staticTableFieldName);

        CodeBlock.Builder pageQueryBuilder = CodeBlock.builder();
        pageQueryBuilder.add("$T queryWrapper = $T.create()\n", QueryWrapper.class, QueryWrapper.class);
        pageQueryBuilder.indent();
        pageQueryBuilder.add(".from($L)\n", tableVarName);

        for (PojoInfo.FieldInfo field : pojoInfo.getFields()) {
            pageQueryBuilder.add(".where($L.$L.$L.eq(query.get$L()))\n",
                    tableVarName, staticTableFieldName, field.getName(), upperFirstChar(field.getName()));
        }

        addTimeRangeConditions(pageQueryBuilder, tableVarName, staticTableFieldName);

        pageQueryBuilder.add(".orderBy($L.$L.$L.desc());\n", tableVarName, staticTableFieldName, idField);
        pageQueryBuilder.unindent();

        pageMethodBuilder.addCode(pageQueryBuilder.build());
        pageMethodBuilder.addStatement("return paginate(page, queryWrapper)");
        interfaceBuilder.addMethod(pageMethodBuilder.build());

        return interfaceBuilder.build();
    }

    private void addTimeRangeConditions(CodeBlock.Builder queryBuilder, String tableVarName, String staticTableFieldName) {
        queryBuilder.add(".where($L.$L.gmtCreate.ge(query.getMinGmtCreate()))\n", tableVarName, staticTableFieldName);
        queryBuilder.add(".where($L.$L.gmtCreate.le(query.getMaxGmtCreate()))\n", tableVarName, staticTableFieldName);
        queryBuilder.add(".where($L.$L.gmtModified.ge(query.getMinGmtModified()))\n", tableVarName, staticTableFieldName);
        queryBuilder.add(".where($L.$L.gmtModified.le(query.getMaxGmtModified()))\n", tableVarName, staticTableFieldName);
    }

    private String upperFirstChar(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    @Override
    public String getPackageName() {
        return packageLayout.getRepositoryPackage();
    }

    @Override
    public String getClassName(PojoInfo pojoInfo) {
        return packageLayout.getRepositoryClassName(pojoInfo.getClassName());
    }
}