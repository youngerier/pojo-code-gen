package io.github.youngerier.generator;

import io.github.youngerier.generator.analysis.SourceCodeAnalyzer;
import io.github.youngerier.generator.generators.CodeGenerator;
import io.github.youngerier.generator.generators.Generators;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.output.CodeFileWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 代码生成引擎，负责协调整个代码生成过程：
 * 解析 POJO 元数据，构建包结构，并依次运行全部代码生成器写出文件。
 */
@Slf4j
public class GeneratorEngine {

    private final GeneratorConfig config;
    private final SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer();

    public GeneratorEngine(GeneratorConfig config) {
        this.config = config;
    }

    /**
     * 执行代码生成。
     */
    public void execute() {
        CodeFileWriter writer = new CodeFileWriter(config.getOutputBaseDir());
        for (Class<?> pojoClass : config.getPojoClasses()) {
            try {
                generateSinglePojo(pojoClass, writer);
            } catch (IOException e) {
                log.error("Error generating code for {}: {}", pojoClass.getName(), e.getMessage(), e);
            }
        }
        log.info("所有代码生成任务完成!");
    }

    private void generateSinglePojo(Class<?> pojoClass, CodeFileWriter writer) throws IOException {
        // 1. 解析 POJO 源码
        ClassMetadata metadata = analyzer.parse(pojoClass, config.getModuleName());
        log.info("Successfully parsed POJO: {}", metadata.getClassName());

        // 2. 推导包结构并创建全部生成器
        PackageStructure packages = new PackageStructure(metadata.getBasePackageName(), metadata.getClassName());

        // 3. 逐一生成文件
        for (CodeGenerator generator : Generators.createAll(packages)) {
            writer.write(generator, metadata);
        }

        log.info("为 {} 生成的代码已完成!", metadata.getClassName());
        log.info("生成的文件位于: {}", writer.sourceOutputRoot().toAbsolutePath());
    }
}
