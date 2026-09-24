package io.github.youngerier.generator;

import io.github.youngerier.generator.analysis.ClassMetadataReader;
import io.github.youngerier.generator.analysis.ModelInput;
import io.github.youngerier.generator.generators.CodeGenerator;
import io.github.youngerier.generator.generators.Generators;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.output.CodeFileWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * 代码生成引擎，负责协调整个代码生成过程：
 * 读取模型元数据，构建包结构，并依次运行全部代码生成器写出文件。
 */
@Slf4j
public class GeneratorEngine {

    private final GeneratorConfig config;
    private final ClassMetadataReader metadataReader = new ClassMetadataReader();

    public GeneratorEngine(GeneratorConfig config) {
        this.config = config;
    }

    /**
     * 执行代码生成。任一模型写文件失败时快速失败，绝不静默跳过。
     */
    public void execute() {
        CodeFileWriter writer = new CodeFileWriter(config.getOutputBaseDir());
        for (ModelInput input : config.getModels()) {
            generateSingle(input, writer);
        }
        log.info("所有代码生成任务完成!");
    }

    private void generateSingle(ModelInput input, CodeFileWriter writer) {
        // 1. 读取 POJO 元数据
        ClassMetadata metadata = metadataReader.read(input);
        log.info("已解析 POJO: {}", metadata.getClassName());

        // 2. 推导包结构并创建全部生成器
        PackageStructure packages = new PackageStructure(
                metadata.getBasePackageName(), metadata.getClassName());

        // 3. 逐一生成文件
        try {
            for (CodeGenerator generator : Generators.createAll(packages)) {
                writer.write(generator, metadata);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("生成代码失败: " + input.qualifiedName(), e);
        }

        log.info("{} 的代码生成完成!", metadata.getClassName());
        log.info("生成的文件位于: {}", writer.sourceOutputRoot().toAbsolutePath());
    }
}
