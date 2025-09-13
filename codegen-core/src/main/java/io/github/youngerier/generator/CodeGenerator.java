package io.github.youngerier.generator;

import io.github.youngerier.generator.model.ClassMetadata;
import com.squareup.javapoet.TypeSpec;

/**
 * 代码生成器接口
 */
public interface CodeGenerator {

    /**
     * 生成代码
     *
     * @param classMetadata 类元数据信息
     * @return 生成的TypeSpec对象
     */
    TypeSpec generate(ClassMetadata classMetadata);

    /**
     * 获取生成文件的包名
     *
     * @return 包名
     */
    String getPackageName();

    /**
     * 获取生成文件的类名
     *
     * @param classMetadata 类元数据信息
     * @return 类名
     */
    String getClassName(ClassMetadata classMetadata);
}
