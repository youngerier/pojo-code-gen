package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.CodeGenerator;
import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.support.Response;
import io.github.youngerier.support.page.Pagination;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Controller控制器生成器
 */
@Slf4j
public class ControllerGenerator implements CodeGenerator {

    private final PackageStructure packageStructure;

    public ControllerGenerator(PackageStructure packageStructure) {
        this.packageStructure = packageStructure;
    }

    @Override
    public TypeSpec generate(ClassMetadata classMetadata) {
        String dtoClassName = packageStructure.getDtoClassName();
        String serviceClassName = packageStructure.getServiceClassName();
        String queryClassName = packageStructure.getQueryClassName();
        
        ClassName dtoType = ClassName.get(packageStructure.getDtoPackage(), dtoClassName);
        ClassName serviceType = ClassName.get(packageStructure.getServicePackage(), serviceClassName);
        ClassName queryType = ClassName.get(packageStructure.getRequestPackage(), queryClassName);
        ClassName responseType = ClassName.get(Response.class);
        ClassName listType = ClassName.get(List.class);
        ClassName paginationType = ClassName.get(Pagination.class);
        
        // 创建类构建器
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(classMetadata))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RestController")).build())
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RequestMapping"))
                        .addMember("value", "$S", "/" + classMetadata.getCamelClassName() + "s")
                        .build())
                .addAnnotation(ClassName.get("lombok", "RequiredArgsConstructor"))
                .addAnnotation(ClassName.get("lombok.extern.slf4j", "Slf4j"));

        // 添加类注释
        if (classMetadata.getClassComment() != null && !classMetadata.getClassComment().isEmpty()) {
            classBuilder.addJavadoc(classMetadata.getClassComment() + "\n");
            classBuilder.addJavadoc("控制器\n");
        }

        // 添加服务字段
        FieldSpec serviceField = FieldSpec.builder(serviceType, classMetadata.getCamelClassName() + "Service")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();
        classBuilder.addField(serviceField);

        // 添加创建方法
        MethodSpec createMethod = MethodSpec.methodBuilder("create" + classMetadata.getClassName())
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, dtoType))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PostMapping")).build())
                .addParameter(ParameterSpec.builder(dtoType, classMetadata.getCamelClassName() + "DTO")
                        .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RequestBody")).build())
                        .build())
                .addJavadoc("创建$L\n", classMetadata.getClassName())
                .addJavadoc("@param $L $L数据传输对象\n",
                        classMetadata.getCamelClassName() + "DTO",
                        classMetadata.getClassName())
                .addJavadoc("@return 创建的$L对象\n", classMetadata.getClassName())
                .addStatement("log.info(\"创建$L: {}\", $L)", classMetadata.getClassName(), classMetadata.getCamelClassName() + "DTO")
                .addStatement("$T result = $L.create$L($L)", 
                        dtoType,
                        classMetadata.getCamelClassName() + "Service",
                        classMetadata.getClassName(),
                        classMetadata.getCamelClassName() + "DTO")
                .addStatement("return $T.ok(result)", responseType)
                .build();
        classBuilder.addMethod(createMethod);

        // 添加根据ID查询方法
        MethodSpec getByIdMethod = MethodSpec.methodBuilder("get" + classMetadata.getClassName() + "ById")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, dtoType))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "GetMapping"))
                        .addMember("value", "$S", "/{id}")
                        .build())
                .addParameter(ParameterSpec.builder(TypeName.LONG, "id")
                        .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PathVariable")).build())
                        .build())
                .addJavadoc("根据ID查询$L\n", classMetadata.getClassName())
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 对应的$L对象\n", classMetadata.getClassName())
                .addStatement("log.info(\"根据ID查询$L: {}\", id)", classMetadata.getClassName())
                .addStatement("$T result = $L.get$LById(id)", 
                        dtoType,
                        classMetadata.getCamelClassName() + "Service",
                        classMetadata.getClassName())
                .addStatement("return $T.ok(result)", responseType)
                .build();
        classBuilder.addMethod(getByIdMethod);

        // 添加查询列表方法
        MethodSpec queryListMethod = MethodSpec.methodBuilder("query" + classMetadata.getClassName() + "List")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, ParameterizedTypeName.get(listType, dtoType)))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PostMapping"))
                        .addMember("value", "$S", "/query")
                        .build())
                .addParameter(ParameterSpec.builder(queryType, "query")
                        .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RequestBody")).build())
                        .build())
                .addJavadoc("查询$L列表\n", classMetadata.getClassName())
                .addJavadoc("@param query 查询条件\n")
                .addJavadoc("@return $L对象列表\n", classMetadata.getClassName())
                .addStatement("log.info(\"查询$L列表: {}\", query)", classMetadata.getClassName())
                .addStatement("$T<$T> result = $L.query$Ls(query)", 
                        listType, dtoType,
                        classMetadata.getCamelClassName() + "Service",
                        classMetadata.getClassName())
                .addStatement("return $T.ok(result)", responseType)
                .build();
        classBuilder.addMethod(queryListMethod);

        // 添加分页查询方法
        MethodSpec pageQueryMethod = MethodSpec.methodBuilder("pageQuery" + classMetadata.getClassName() + "s")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, ParameterizedTypeName.get(paginationType, dtoType)))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PostMapping"))
                        .addMember("value", "$S", "/page")
                        .build())
                .addParameter(ParameterSpec.builder(queryType, "query")
                        .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RequestBody")).build())
                        .build())
                .addJavadoc("分页查询$L\n", classMetadata.getClassName())
                .addJavadoc("@param query 查询条件\n")
                .addJavadoc("@return $L分页对象\n", classMetadata.getClassName())
                .addStatement("log.info(\"分页查询$L: {}\", query)", classMetadata.getClassName())
                .addStatement("$T<$T> result = $L.pageQuery$Ls(query)", 
                        paginationType, dtoType,
                        classMetadata.getCamelClassName() + "Service",
                        classMetadata.getClassName())
                .addStatement("return $T.ok(result)", responseType)
                .build();
        classBuilder.addMethod(pageQueryMethod);

        // 添加更新方法
        MethodSpec updateMethod = MethodSpec.methodBuilder("update" + classMetadata.getClassName())
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, dtoType))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PutMapping")).build())
                .addParameter(ParameterSpec.builder(dtoType, classMetadata.getCamelClassName() + "DTO")
                        .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RequestBody")).build())
                        .build())
                .addJavadoc("更新$L\n", classMetadata.getClassName())
                .addJavadoc("@param $L $L数据传输对象\n",
                        classMetadata.getCamelClassName() + "DTO",
                        classMetadata.getClassName())
                .addJavadoc("@return 更新后的$L对象\n", classMetadata.getClassName())
                .addStatement("log.info(\"更新$L: id={}, data={}\", $L.getId(), $L)", classMetadata.getClassName(), classMetadata.getCamelClassName() + "DTO", classMetadata.getCamelClassName() + "DTO")
                .addStatement("$T result = $L.update$L($L.getId(), $L)", 
                        dtoType,
                        classMetadata.getCamelClassName() + "Service",
                        classMetadata.getClassName(),
                        classMetadata.getCamelClassName() + "DTO",
                        classMetadata.getCamelClassName() + "DTO")
                .addStatement("return $T.ok(result)", responseType)
                .build();
        classBuilder.addMethod(updateMethod);

        // 添加删除方法
        MethodSpec deleteMethod = MethodSpec.methodBuilder("delete" + classMetadata.getClassName())
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, ClassName.get(Boolean.class)))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "DeleteMapping"))
                        .addMember("value", "$S", "/{id}")
                        .build())
                .addParameter(ParameterSpec.builder(TypeName.LONG, "id")
                        .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PathVariable")).build())
                        .build())
                .addJavadoc("删除$L\n", classMetadata.getClassName())
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 是否删除成功\n")
                .addStatement("log.info(\"删除$L: id={}\", id)", classMetadata.getClassName())
                .addStatement("boolean result = $L.delete$L(id)", 
                        classMetadata.getCamelClassName() + "Service",
                        classMetadata.getClassName())
                .addStatement("return $T.ok(result)", responseType)
                .build();
        classBuilder.addMethod(deleteMethod);

        return classBuilder.build();
    }

    @Override
    public String getPackageName() {
        return packageStructure.getControllerPackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getControllerClassName();
    }
}