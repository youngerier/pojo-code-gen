package com.abc;

import com.example.generator.GeneratorConfig;
import com.example.generator.GeneratorEngine;

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
                .pojoPaths(Collections.singletonList(resolvePojoPath(pojoClassName, moduleName)))
                .build();

        // 3. 创建并执行引擎
        GeneratorEngine engine = new GeneratorEngine(config);
        engine.execute();
    }

    /**
     * 将类名转换为项目内的完整文件路径。
     */
    private static String resolvePojoPath(String className, String moduleName) {
        String filePath = className.replace('.', File.separatorChar) + ".java";
        return new File(System.getProperty("user.dir"), moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator + filePath).getAbsolutePath();
    }
}
