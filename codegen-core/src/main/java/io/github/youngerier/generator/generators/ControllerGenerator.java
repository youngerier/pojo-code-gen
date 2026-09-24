package io.github.youngerier.generator.generators;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.youngerier.support.Response;
import io.github.youngerier.support.page.Pagination;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.GeneratedType;
import io.github.youngerier.generator.model.PackageStructure;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Controller 控制器生成器
 */
public class ControllerGenerator extends BaseGenerator {

    private static final String WEB_PACKAGE = "org.springframework.web.bind.annotation";

    public ControllerGenerator(PackageStructure packageStructure) {
        super(packageStructure, GeneratedType.CONTROLLER);
    }

    @Override
    public TypeSpec generate(ClassMetadata metadata) {
        String entityName = metadata.getClassName();
        String camelName = metadata.getCamelClassName();
        String dtoParameter = camelName + "DTO";
        String serviceField = camelName + "Service";

        ClassName dtoType = packages.dto();
        ClassName queryType = packages.query();
        // 主键类型以 @Id 字段实际类型为准；未标注 @Id 时回退 Long（保持历史约定）
        ClassMetadata.FieldInfo primaryKey = metadata.getPrimaryKey();
        TypeName idType = primaryKey != null ? primaryKey.getType() : TypeName.LONG;
        ClassName responseType = ClassName.get(Response.class);
        ClassName paginationType = ClassName.get(Pagination.class);
        ParameterizedTypeName responseOfDto = ParameterizedTypeName.get(responseType, dtoType);

        TypeSpec.Builder builder = TypeSpec.classBuilder(getClassName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(web("RestController"))
                .addAnnotation(AnnotationSpec.builder(web("RequestMapping"))
                        .addMember("value", "$S", "/" + camelName + "s")
                        .build())
                .addAnnotation(ClassName.get("lombok", "RequiredArgsConstructor"))
                .addAnnotation(ClassName.get("lombok.extern.slf4j", "Slf4j"));

        builder.addField(FieldSpec.builder(packages.service(), serviceField)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build());

        // 创建
        builder.addMethod(MethodSpec.methodBuilder("create" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(responseOfDto)
                .addAnnotation(web("PostMapping"))
                .addParameter(annotatedParameter(dtoType, dtoParameter, "RequestBody"))
                .addJavadoc("创建$L\n", entityName)
                .addJavadoc("@param $L $L数据传输对象\n", dtoParameter, entityName)
                .addJavadoc("@return 创建的$L对象\n", entityName)
                .addStatement("log.info(\"创建$L: {}\", $L)", entityName, dtoParameter)
                .addStatement("$T result = $L.create$L($L)", dtoType, serviceField, entityName, dtoParameter)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // 根据 ID 查询
        builder.addMethod(MethodSpec.methodBuilder("get" + entityName + "ById")
                .addModifiers(Modifier.PUBLIC)
                .returns(responseOfDto)
                .addAnnotation(AnnotationSpec.builder(web("GetMapping"))
                        .addMember("value", "$S", "/{id}")
                        .build())
                .addParameter(annotatedParameter(idType, "id", "PathVariable"))
                .addJavadoc("根据ID查询$L\n", entityName)
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 对应的$L对象\n", entityName)
                .addStatement("log.info(\"根据ID查询$L: {}\", id)", entityName)
                .addStatement("$T result = $L.get$LById(id)", dtoType, serviceField, entityName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // 查询列表
        builder.addMethod(MethodSpec.methodBuilder("query" + entityName + "List")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType,
                        ParameterizedTypeName.get(ClassName.get(List.class), dtoType)))
                .addAnnotation(AnnotationSpec.builder(web("PostMapping"))
                        .addMember("value", "$S", "/query")
                        .build())
                .addParameter(annotatedParameter(queryType, "query", "RequestBody"))
                .addJavadoc("查询$L列表\n", entityName)
                .addJavadoc("@param query 查询条件\n")
                .addJavadoc("@return $L对象列表\n", entityName)
                .addStatement("log.info(\"查询$L列表: {}\", query)", entityName)
                .addStatement("$T<$T> result = $L.query$Ls(query)",
                        ClassName.get(List.class), dtoType, serviceField, entityName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // 分页查询
        builder.addMethod(MethodSpec.methodBuilder("pageQuery" + entityName + "s")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType,
                        ParameterizedTypeName.get(paginationType, dtoType)))
                .addAnnotation(AnnotationSpec.builder(web("PostMapping"))
                        .addMember("value", "$S", "/page")
                        .build())
                .addParameter(annotatedParameter(queryType, "query", "RequestBody"))
                .addJavadoc("分页查询$L\n", entityName)
                .addJavadoc("@param query 查询条件\n")
                .addJavadoc("@return $L分页对象\n", entityName)
                .addStatement("log.info(\"分页查询$L: {}\", query)", entityName)
                .addStatement("$T<$T> result = $L.pageQuery$Ls(query)",
                        paginationType, dtoType, serviceField, entityName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // 更新
        builder.addMethod(MethodSpec.methodBuilder("update" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(responseOfDto)
                .addAnnotation(web("PutMapping"))
                .addParameter(annotatedParameter(dtoType, dtoParameter, "RequestBody"))
                .addJavadoc("更新$L\n", entityName)
                .addJavadoc("@param $L $L数据传输对象\n", dtoParameter, entityName)
                .addJavadoc("@return 更新后的$L对象\n", entityName)
                .addStatement("log.info(\"更新$L: id={}, data={}\", $L.getId(), $L)",
                        entityName, dtoParameter, dtoParameter)
                .addStatement("$T result = $L.update$L($L.getId(), $L)",
                        dtoType, serviceField, entityName, dtoParameter, dtoParameter)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // 删除
        builder.addMethod(MethodSpec.methodBuilder("delete" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, ClassName.get(Boolean.class)))
                .addAnnotation(AnnotationSpec.builder(web("DeleteMapping"))
                        .addMember("value", "$S", "/{id}")
                        .build())
                .addParameter(annotatedParameter(idType, "id", "PathVariable"))
                .addJavadoc("删除$L\n", entityName)
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 是否删除成功\n")
                .addStatement("log.info(\"删除$L: id={}\", id)", entityName)
                .addStatement("boolean result = $L.delete$L(id)", serviceField, entityName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        Javadocs.appendClassComment(builder, metadata, "控制器");
        return builder.build();
    }

    private static ClassName web(String simpleName) {
        return ClassName.get(WEB_PACKAGE, simpleName);
    }

    private static ParameterSpec annotatedParameter(TypeName type, String name, String annotation) {
        return ParameterSpec.builder(type, name)
                .addAnnotation(AnnotationSpec.builder(web(annotation)).build())
                .build();
    }
}
