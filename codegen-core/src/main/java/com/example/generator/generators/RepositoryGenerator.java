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
import java.util.function.Consumer;

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
        return buildQueryMethod(pojoInfo, "selectListByQuery", 
                methodBuilder -> {
                    ClassName entityType = getEntityType(pojoInfo);
                    methodBuilder.returns(ParameterizedTypeName.get(ClassName.get(List.class), entityType));
                },
                methodBuilder -> methodBuilder.addStatement("return selectListByQuery(queryWrapper)"));
    }

    private MethodSpec buildPageMethod(PojoInfo pojoInfo) {
        return buildQueryMethod(pojoInfo, "page",
                methodBuilder -> {
                    ClassName entityType = getEntityType(pojoInfo);
                    methodBuilder.returns(ParameterizedTypeName.get(ClassName.get(Page.class), entityType));
                },
                methodBuilder -> {
                    ClassName entityType = getEntityType(pojoInfo);
                    methodBuilder.addStatement("$T<$T> page = new Page<>(query.getQueryPage(), query.getQuerySize())", Page.class, entityType);
                    methodBuilder.addStatement("return paginate(page, queryWrapper)");
                });
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
     * 构建查询方法的通用模板
     *
     * @param pojoInfo 实体信息
     * @param methodName 方法名
     * @param returnTypeConfig 返回类型配置
     * @param bodyConfig 方法体配置
     * @return 方法规范
     */
    private MethodSpec buildQueryMethod(PojoInfo pojoInfo, String methodName, 
                                       Consumer<MethodSpec.Builder> returnTypeConfig,
                                       Consumer<MethodSpec.Builder> bodyConfig) {
        ClassName queryType = getQueryType(pojoInfo);
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(queryType, "query");
        
        returnTypeConfig.accept(methodBuilder);
        buildQueryWrapperLogic(methodBuilder, pojoInfo);
        bodyConfig.accept(methodBuilder);
        
        return methodBuilder.build();
    }

    /**
     * 获取实体类型
     *
     * @param pojoInfo 实体信息
     * @return 实体类型
     */
    private ClassName getEntityType(PojoInfo pojoInfo) {
        return ClassName.get(pojoInfo.getPackageName(), pojoInfo.getClassName());
    }

    /**
     * 获取查询类型
     *
     * @param pojoInfo 实体信息
     * @return 查询类型
     */
    private ClassName getQueryType(PojoInfo pojoInfo) {
        return ClassName.get(packageLayout.getRequestPackage(), pojoInfo.getClassName() + "Query");
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
