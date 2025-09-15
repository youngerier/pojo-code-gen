package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.CodeGenerator;
import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import javax.lang.model.element.Modifier;

/**
 * Mapper接口生成器 - 基于MyBatis Flex
 */
public class MapperGenerator implements CodeGenerator {
    private final PackageStructure packageLayout;

    public MapperGenerator(PackageStructure packageLayout) {
        this.packageLayout = packageLayout;
    }

    @Override
    public TypeSpec generate(ClassMetadata pojoInfo) {
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(getClassName(pojoInfo))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Mapper.class)
                .addSuperinterface(createBaseMapperType(pojoInfo));

        if (pojoInfo.getClassComment() != null && !pojoInfo.getClassComment().isEmpty()) {
            interfaceBuilder.addJavadoc(pojoInfo.getClassComment() + "\n");
            interfaceBuilder.addJavadoc("数据访问层Mapper接口\n");
        }

        return interfaceBuilder.build();
    }

    private ParameterizedTypeName createBaseMapperType(ClassMetadata pojoInfo) {
        ClassName entityType = ClassName.get(pojoInfo.getPackageName(), pojoInfo.getClassName());
        ClassName baseMapperType = ClassName.get("com.mybatisflex.core", "BaseMapper");
        return ParameterizedTypeName.get(baseMapperType, entityType);
    }

    @Override
    public String getPackageName() {
        return packageLayout.getMapperPackage();
    }

    @Override
    public String getClassName(ClassMetadata pojoInfo) {
        return packageLayout.getMapperClassName();
    }
}