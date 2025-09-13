package com.abc;

import io.github.youngerier.generator.GeneratorConfig;
import io.github.youngerier.generator.GeneratorEngine;

import java.io.File;
import java.util.Collections;

/**
 * 代码生成器主程序 - 示例
 */
public class CodeGeneratorMain {

    public static void main(String[] args) {
        // 1. 定义配置
        String moduleName = "example";
        String pojoClassName = "com.abc.entity.User";

        // 2. 构建 GeneratorConfig
        GeneratorConfig config = GeneratorConfig.builder()
                .moduleName(moduleName)
                .outputBaseDir("target" + File.separator + "generated-sources")
                .pojoClasses(Collections.singletonList(pojoClassName))
                .build();

        // 3. 创建并执行引擎
        GeneratorEngine engine = new GeneratorEngine(config);
        engine.execute();
    }
}
