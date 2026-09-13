package io.github.youngerier.generator.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.GeneratedType;
import io.github.youngerier.generator.model.PackageStructure;
import org.apache.ibatis.annotations.Mapper;

import javax.lang.model.element.Modifier;

/**
 * Mapper 接口生成器 - 基于 MyBatis Flex
 */
public class MapperGenerator extends BaseGenerator {

    private static final ClassName BASE_MAPPER = ClassName.get("com.mybatisflex.core", "BaseMapper");

    public MapperGenerator(PackageStructure packageLayout) {
        super(packageLayout, GeneratedType.MAPPER);
    }

    @Override
    public TypeSpec generate(ClassMetadata metadata) {
        ClassName entityType = ClassName.get(metadata.getPackageName(), metadata.getClassName());

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(getClassName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Mapper.class)
                .addSuperinterface(ParameterizedTypeName.get(BASE_MAPPER, entityType));

        Javadocs.appendClassComment(builder, metadata, "数据访问层Mapper接口");
        return builder.build();
    }
}
