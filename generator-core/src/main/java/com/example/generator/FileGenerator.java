package com.example.generator;

import com.example.generator.model.PojoInfo;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 文件生成器，负责将生成的代码写入文件系统
 */
@Slf4j
public class FileGenerator {

    private static final String DEFAULT_INDENT = "    "; // 默认4个空格缩进
    
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
        String packageName = codeGenerator.getPackageName();
        String className = codeGenerator.getClassName(pojoInfo);

        // 生成TypeSpec
        TypeSpec typeSpec = codeGenerator.generate(pojoInfo);

        // 创建JavaFile
        JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                .indent(DEFAULT_INDENT)
                .build();

        // 计算新内容的哈希值
        byte[] newContentBytes = javaFile.toString().getBytes(StandardCharsets.UTF_8);
        byte[] newHash = calculateHash(newContentBytes);

        // 检查文件是否已存在
        Path outputDirPath = Paths.get(baseOutputDir, "src", "main", "java");
        Path packagePath = outputDirPath;
        if (!packageName.isEmpty()) {
            for (String packageComponent : packageName.split("\\.")) {
                packagePath = packagePath.resolve(packageComponent);
            }
        }
        Path filePath = packagePath.resolve(className + ".java");

        if (filePath.toFile().exists()) {
            byte[] existingContentBytes = Files.readAllBytes(filePath);
            byte[] existingHash = calculateHash(existingContentBytes);
            if (Arrays.equals(newHash, existingHash)) {
                log.info("文件内容未改变，跳过生成: {}", filePath);
                return;
            }
        }

        // 写入文件
        javaFile.writeTo(outputDirPath.toFile());

        log.info("生成文件: {}.{}", packageName, className);
    }

    private byte[] calculateHash(byte[] content) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return digest.digest(content);
    }
}
