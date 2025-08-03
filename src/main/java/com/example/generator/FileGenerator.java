package com.example.generator;

import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * 文件生成器，负责将生成的代码写入文件系统
 */
@Slf4j
public class FileGenerator {

    private final String baseOutputDir;

    public FileGenerator(String baseOutputDir) {
        this.baseOutputDir = baseOutputDir;
    }

    /**
     * 生成文件
     *
     * @param codeGenerator 代码生成器
     * @param pojoInfo      POJO信息
     * @throws IOException IO异常
     */
    public void generateFile(CodeGenerator codeGenerator, PojoInfo pojoInfo) throws IOException {
        // 生成TypeSpec
        TypeSpec typeSpec = codeGenerator.generate(pojoInfo);

        // 创建JavaFile
        JavaFile javaFile = JavaFile.builder(codeGenerator.getPackageName(), typeSpec)
                .indent("    ") // 使用4个空格缩进
                .build();

        // 写入文件
        File outputDir = Paths.get(baseOutputDir, "src", "main", "java").toFile();
        javaFile.writeTo(outputDir);

        log.info("生成文件: {}.{}", codeGenerator.getPackageName(), codeGenerator.getClassName(pojoInfo));
    }
}
