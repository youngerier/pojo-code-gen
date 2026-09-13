package io.github.youngerier.generator.generators;

import com.squareup.javapoet.TypeSpec;
import io.github.youngerier.generator.model.ClassMetadata;

/**
 * 代码生成器接口
 */
public interface CodeGenerator {

    /**
     * 生成代码
     *
     * @param classMetadata 类元数据信息
     * @return 生成的 TypeSpec 对象
     */
    TypeSpec generate(ClassMetadata classMetadata);

    /**
     * 获取生成文件的包名
     */
    String getPackageName();

    /**
     * 获取生成文件的类名
     */
    String getClassName();
}
