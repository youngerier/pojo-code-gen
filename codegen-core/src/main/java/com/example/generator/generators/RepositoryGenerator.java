package com.example.generator.generators;

import com.abc.web.support.QueryWrapperHelper;
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
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(createBaseMapperType(pojoInfo));

        if (pojoInfo.getClassComment() != null && !pojoInfo.getClassComment().isEmpty()) {
            interfaceBuilder.addJavadoc(pojoInfo.getClassComment() + "\n");
            interfaceBuilder.addJavadoc("数据访问层接口\n");
        }

        interfaceBuilder.addMethod(buildSelectListByQueryMethod(pojoInfo));
        interfaceBuilder.addMethod(buildPageMethod(pojoInfo));

        return interfaceBuilder.build();
    }

    private ParameterizedTypeName createBaseMapperType(PojoInfo pojoInfo) {
        ClassName entityType = ClassName.get(pojoInfo.getPackageName(), pojoInfo.getClassName());
        ClassName baseMapperType = ClassName.get("com.mybatisflex.core", "BaseMapper");
        return ParameterizedTypeName.get(baseMapperType, entityType);
    }

    private MethodSpec buildSelectListByQueryMethod(PojoInfo pojoInfo) {
        ClassName entityType = ClassName.get(pojoInfo.getPackageName(), pojoInfo.getClassName());
        ClassName queryType = ClassName.get(packageLayout.getRequestPackage(), pojoInfo.getClassName() + "Query");

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("selectListByQuery")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(queryType, "query")
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), entityType));

        buildQueryWrapperLogic(methodBuilder, pojoInfo);

        methodBuilder.addStatement("return selectListByQuery(queryWrapper)");
        return methodBuilder.build();
    }

    private MethodSpec buildPageMethod(PojoInfo pojoInfo) {
        ClassName entityType = ClassName.get(pojoInfo.getPackageName(), pojoInfo.getClassName());
        ClassName queryType = ClassName.get(packageLayout.getRequestPackage(), pojoInfo.getClassName() + "Query");

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("page")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(queryType, "query")
                .returns(ParameterizedTypeName.get(ClassName.get(Page.class), entityType));

        methodBuilder.addStatement("$T<$T> page = new Page<>(query.getQueryPage(), query.getQuerySize())", Page.class, entityType);
        buildQueryWrapperLogic(methodBuilder, pojoInfo);

        methodBuilder.addStatement("return paginate(page, queryWrapper)");
        return methodBuilder.build();
    }

    private void buildQueryWrapperLogic(MethodSpec.Builder methodBuilder, PojoInfo pojoInfo) {
        ClassName tableRefs = ClassName.get(pojoInfo.getPackageName() + ".table", pojoInfo.getClassName() + "TableRefs");
        String tableVarName = toCamelCase(pojoInfo.getClassName()) + "TableRefs";
        String staticTableFieldName = toCamelCase(pojoInfo.getClassName());

        methodBuilder.addStatement("$T $L = $T.$L", tableRefs, tableVarName, tableRefs, staticTableFieldName);

        CodeBlock.Builder queryWrapperBuilder = CodeBlock.builder();
        queryWrapperBuilder.add("$T queryWrapper = $T.withOrder(query)\n", QueryWrapper.class, QueryWrapperHelper.class);
        queryWrapperBuilder.indent();
        queryWrapperBuilder.add(".from($L)\n", tableVarName);

        for (PojoInfo.FieldInfo field : pojoInfo.getFields()) {
            String fieldName = field.getName();
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            queryWrapperBuilder.add(".where($L.$L.eq(query.$L()))\n",
                    tableVarName, fieldName, getterName);
        }

        queryWrapperBuilder.add(".and($L.gmtCreate.ge(query.getMinGmtCreate()))\n", tableVarName);
        queryWrapperBuilder.add(".and($L.gmtCreate.le(query.getMaxGmtCreate()))\n", tableVarName);
        queryWrapperBuilder.add(".and($L.gmtModified.ge(query.getMinGmtModified()))\n", tableVarName);
        queryWrapperBuilder.add(".and($L.gmtModified.le(query.getMaxGmtModified()));\n", tableVarName);
        queryWrapperBuilder.unindent();

        methodBuilder.addCode(queryWrapperBuilder.build());
    }

    @Override
    public String getPackageName() {
        return packageLayout.getRepositoryPackage();
    }

    @Override
    public String getClassName(PojoInfo pojoInfo) {
        return packageLayout.getRepositoryClassName(pojoInfo.getClassName());
    }

    /**
     * 将类名转换为驼峰命名（首字母小写）
     *
     * @param className 类名
     * @return 驼峰命名的字符串
     */
    private static String toCamelCase(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
}
