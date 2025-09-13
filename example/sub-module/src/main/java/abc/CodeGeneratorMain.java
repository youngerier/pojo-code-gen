package com.abc;

import io.github.youngerier.generator.GeneratorConfig;
import io.github.youngerier.generator.GeneratorEngine;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Collections;

/**
 * 代码生成器主程序 - 示例
 */
@Slf4j
public class CodeGeneratorMain {

    public static void main(String[] args) {
        // 1. 定义配置
        String moduleName = "example";
        String pojoClassName = "com.abc.entity.User";

        try {
            Class<?> pojoClass = Class.forName(pojoClassName);

            // 2. 构建 GeneratorConfig
            GeneratorConfig config = GeneratorConfig.builder()
                    .moduleName(moduleName)
                    .outputBaseDir("target" + File.separator + "generated-sources")
                    .pojoClasses(Collections.singletonList(pojoClass))
                    .build();

            // 3. 创建并执行引擎
            GeneratorEngine engine = new GeneratorEngine(config);
            engine.execute();
        } catch (ClassNotFoundException e) {
            log.error("找不到指定的POJO类: {}", pojoClassName,e);
        }
    }
}