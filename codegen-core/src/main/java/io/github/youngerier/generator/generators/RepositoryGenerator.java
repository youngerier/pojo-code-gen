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
import com.mybatisflex.core.service.IService;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Repository实现类生成器 - 基于MyBatis Flex ServiceImpl
 */
@Slf4j
public class RepositoryGenerator implements CodeGenerator {
    private final PackageStructure packageLayout;

    public RepositoryGenerator(PackageStructure packageLayout) {
        this.packageLayout = packageLayout;
    }

    @Override
    public TypeSpec generate(ClassMetadata pojoInfo) {
        // 创建实体类类型
        ClassName entityType = ClassName.get(pojoInfo.getPackageName(), pojoInfo.getClassName());
        // 创建Mapper类型
        ClassName mapperType = ClassName.get(packageLayout.getMapperPackage(), packageLayout.getMapperClassName());
        // 创建ServiceImpl类型
        ClassName serviceImplType = ClassName.get("com.mybatisflex.spring.service.impl", "ServiceImpl");
        // 创建IService类型
        ClassName serviceType = ClassName.get("com.mybatisflex.core.service", "IService");
        
        // 创建类构建器，继承ServiceImpl<UserMapper, User>
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(serviceImplType, mapperType, entityType))
                .addSuperinterface(ParameterizedTypeName.get(serviceType, entityType));

        if (pojoInfo.getClassComment() != null && !pojoInfo.getClassComment().isEmpty()) {
            classBuilder.addJavadoc(pojoInfo.getClassComment() + "\n");
            classBuilder.addJavadoc("数据访问层实现类\n");
        }

        classBuilder.addMethod(buildQueryWrapperMethod(pojoInfo));
        classBuilder.addMethod(buildSelectListByQueryMethod(pojoInfo));
        classBuilder.addMethod(buildPageMethod(pojoInfo));

        return classBuilder.build();
    }

    private MethodSpec buildSelectListByQueryMethod(ClassMetadata pojoInfo) {
        ClassName entityType = getEntityType(pojoInfo);
        ClassName queryType = getQueryType(pojoInfo);

        return MethodSpec.methodBuilder("selectListByQuery")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(queryType, "query")
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), entityType))
                .addStatement("return getMapper().selectListByQuery(buildQueryWrapper(query))")
                .build();
    }

    private MethodSpec buildPageMethod(ClassMetadata pojoInfo) {
        ClassName entityType = getEntityType(pojoInfo);
        ClassName queryType = getQueryType(pojoInfo);

        return MethodSpec.methodBuilder("page")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(queryType, "query")
                .returns(ParameterizedTypeName.get(ClassName.get(Page.class), entityType))
                .addStatement("$T<$T> page = new $T<>(query.getQueryPage(), query.getQuerySize())", 
                    ClassName.get(Page.class), entityType, ClassName.get(Page.class))
                .addStatement("return getMapper().paginate(page, buildQueryWrapper(query))")
                .build();
    }

    private MethodSpec buildQueryWrapperMethod(ClassMetadata pojoInfo) {
        ClassName queryType = getQueryType(pojoInfo);
        ClassName tableRefs = ClassName.get(pojoInfo.getPackageName() + ".table", pojoInfo.getClassName() + "TableRefs");
        String tableVarName = toCamelCase(pojoInfo.getClassName()) + "TableRefs";
        String staticTableFieldName = toCamelCase(pojoInfo.getClassName());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("buildQueryWrapper")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(queryType, "query")
                .returns(QueryWrapper.class);

        methodBuilder.addStatement("$T $L = $T.$L", tableRefs, tableVarName, tableRefs, staticTableFieldName);

        CodeBlock.Builder queryWrapperBuilder = CodeBlock.builder();
        queryWrapperBuilder.add("return $T.withOrder(query)\n", QueryWrapperHelper.class);
        queryWrapperBuilder.indent();
        queryWrapperBuilder.add(".from($L)\n", tableVarName);

        // Add conditional where clauses for each field
        boolean firstField = true;
        for (ClassMetadata.FieldInfo field : pojoInfo.getFields()) {
            String fieldName = field.getName();
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            // 为每个字段添加条件查询
            if (firstField) {
                queryWrapperBuilder.add(".where($L.$L.eq(query.$L()))\n",
                        tableVarName, fieldName, getterName);
                firstField = false;
            } else {
                queryWrapperBuilder.add(".and($L.$L.eq(query.$L()))\n",
                        tableVarName, fieldName, getterName);
            }
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