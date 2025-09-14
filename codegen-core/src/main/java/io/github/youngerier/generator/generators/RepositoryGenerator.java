package io.github.youngerier.generator.generators;

import io.github.youngerier.support.QueryWrapperHelper;
import io.github.youngerier.generator.CodeGenerator;
import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
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
    private final PackageStructure packageLayout;

    public RepositoryGenerator(PackageStructure packageLayout) {
        this.packageLayout = packageLayout;
    }

    @Override
    public TypeSpec generate(ClassMetadata pojoInfo) {
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(createBaseMapperType(pojoInfo));

        if (pojoInfo.getClassComment() != null && !pojoInfo.getClassComment().isEmpty()) {
            interfaceBuilder.addJavadoc(pojoInfo.getClassComment() + "\n");
            interfaceBuilder.addJavadoc("数据访问层接口\n");
        }

        interfaceBuilder.addMethod(buildQueryWrapperMethod(pojoInfo));
        interfaceBuilder.addMethod(buildSelectListByQueryMethod(pojoInfo));
        interfaceBuilder.addMethod(buildPageMethod(pojoInfo));

        return interfaceBuilder.build();
    }

    private ParameterizedTypeName createBaseMapperType(ClassMetadata pojoInfo) {
        ClassName entityType = ClassName.get(pojoInfo.getPackageName(), pojoInfo.getClassName());
        ClassName baseMapperType = ClassName.get("com.mybatisflex.core", "BaseMapper");
        return ParameterizedTypeName.get(baseMapperType, entityType);
    }

    private MethodSpec buildSelectListByQueryMethod(ClassMetadata pojoInfo) {
        ClassName entityType = getEntityType(pojoInfo);
        ClassName queryType = getQueryType(pojoInfo);

        return MethodSpec.methodBuilder("selectListByQuery")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(queryType, "query")
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), entityType))
                .addStatement("return selectListByQuery(buildQueryWrapper(query))")
                .build();
    }

    private MethodSpec buildPageMethod(ClassMetadata pojoInfo) {
        ClassName entityType = getEntityType(pojoInfo);
        ClassName queryType = getQueryType(pojoInfo);

        return MethodSpec.methodBuilder("page")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(queryType, "query")
                .returns(ParameterizedTypeName.get(ClassName.get(Page.class), entityType))
                .addStatement("$T<$T> page = new Page<>(query.getQueryPage(), query.getQuerySize())", Page.class, entityType)
                .addStatement("return paginate(page, buildQueryWrapper(query))")
                .build();
    }

    private MethodSpec buildQueryWrapperMethod(ClassMetadata pojoInfo) {
        ClassName queryType = getQueryType(pojoInfo);
        ClassName tableRefs = ClassName.get(pojoInfo.getPackageName() + ".table", pojoInfo.getClassName() + "TableRefs");
        String tableVarName = toCamelCase(pojoInfo.getClassName()) + "TableRefs";
        String staticTableFieldName = toCamelCase(pojoInfo.getClassName());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("buildQueryWrapper")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(queryType, "query")
                .returns(QueryWrapper.class);

        methodBuilder.addStatement("$T $L = $T.$L", tableRefs, tableVarName, tableRefs, staticTableFieldName);

        CodeBlock.Builder queryWrapperBuilder = CodeBlock.builder();
        queryWrapperBuilder.add("return $T.withOrder(query)\n", QueryWrapperHelper.class);
        queryWrapperBuilder.indent();
        queryWrapperBuilder.add(".from($L)\n", tableVarName);

        // Add conditional where clauses for each field
        for (ClassMetadata.FieldInfo field : pojoInfo.getFields()) {
            String fieldName = field.getName();
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            queryWrapperBuilder.add(".where($L.$L.eq(query.$L()))\n",
                    tableVarName, fieldName, getterName);
        }

        // Add time range conditions
        queryWrapperBuilder.add(".and($L.gmtCreate.ge(query.getMinGmtCreate()))\n", tableVarName);
        queryWrapperBuilder.add(".and($L.gmtCreate.le(query.getMaxGmtCreate()))\n", tableVarName);
        queryWrapperBuilder.add(".and($L.gmtModified.ge(query.getMinGmtModified()))\n", tableVarName);
        queryWrapperBuilder.add(".and($L.gmtModified.le(query.getMaxGmtModified()));\n", tableVarName);
        queryWrapperBuilder.unindent();

        methodBuilder.addCode(queryWrapperBuilder.build());
        return methodBuilder.build();
    }



    @Override
    public String getPackageName() {
        return packageLayout.getRepositoryPackage();
    }

    @Override
    public String getClassName(ClassMetadata pojoInfo) {
        return packageLayout.getRepositoryClassName();
    }



    /**
     * 获取实体类型
     *
     * @param pojoInfo 实体信息
     * @return 实体类型
     */
    private ClassName getEntityType(ClassMetadata pojoInfo) {
        return ClassName.get(pojoInfo.getPackageName(), pojoInfo.getClassName());
    }

    /**
     * 获取查询类型
     *
     * @param pojoInfo 实体信息
     * @return 查询类型
     */
    private ClassName getQueryType(ClassMetadata pojoInfo) {
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
