package io.github.youngerier.generator.output;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.github.youngerier.generator.generators.CodeGenerator;
import io.github.youngerier.generator.model.ClassMetadata;
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
 * 代码文件写入器，负责将生成的代码写入文件系统。
 *
 * <p>写入前比对新旧内容的 SHA-256，内容未变化时跳过，避免无意义的文件覆盖。
 */
@Slf4j
public class CodeFileWriter {

    private static final Path SRC_MAIN_JAVA = Paths.get("src", "main", "java");
    private static final String DEFAULT_INDENT = "    ";

    private final Path sourceOutputRoot;

    public CodeFileWriter(String baseOutputDir) {
        this.sourceOutputRoot = Paths.get(baseOutputDir).resolve(SRC_MAIN_JAVA);
    }

    /**
     * 生成代码并写入文件。
     */
    public void write(CodeGenerator generator, ClassMetadata classMetadata) throws IOException {
        String packageName = generator.getPackageName();
        String className = generator.getClassName();

        TypeSpec typeSpec = generator.generate(classMetadata);
        JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                .indent(DEFAULT_INDENT)
                .build();

        Path filePath = resolveFilePath(packageName, className);
        byte[] newContent = javaFile.toString().getBytes(StandardCharsets.UTF_8);

        if (Files.exists(filePath) && Arrays.equals(sha256(newContent), sha256(Files.readAllBytes(filePath)))) {
            log.info("文件内容未改变，跳过生成: {}", filePath);
            return;
        }

        javaFile.writeTo(sourceOutputRoot.toFile());
        log.info("生成文件: {}.{}", packageName, className);
    }

    /**
     * 生成源码的根目录（baseOutputDir/src/main/java）。
     */
    public Path sourceOutputRoot() {
        return sourceOutputRoot;
    }

    private Path resolveFilePath(String packageName, String className) {
        Path packagePath = sourceOutputRoot;
        if (!packageName.isEmpty()) {
            for (String packageComponent : packageName.split("\\.")) {
                packagePath = packagePath.resolve(packageComponent);
            }
        }
        return packagePath.resolve(className + ".java");
    }

    private static byte[] sha256(byte[] content) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(content);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
