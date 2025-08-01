package com.example.generator;

import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.TypeSpec;

/**
 * 代码生成器接口
 */
public interface CodeGenerator {

    /**
     * 生成代码
     * @param pojoInfo POJO信息
     * @return 生成的TypeSpec对象
     */
    TypeSpec generate(PojoInfo pojoInfo);

    /**
     * 获取生成文件的包名
     * @param pojoInfo POJO信息
     * @return 包名
     */
    String getPackageName(PojoInfo pojoInfo);

    /**
     * 获取生成文件的类名
     * @param pojoInfo POJO信息
     * @return 类名
     */
    String getClassName(PojoInfo pojoInfo);
}
