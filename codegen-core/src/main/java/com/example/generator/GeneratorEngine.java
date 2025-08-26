package com.example.generator;

import com.example.generator.generators.*;
import com.example.generator.model.PackageStructure;
import com.example.generator.model.ClassMetadata;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * 代码生成引擎，负责协调整个代码生成过程。
 */
@Slf4j
public class GeneratorEngine {

    private final GeneratorConfig config;

    public GeneratorEngine(GeneratorConfig config) {
        this.config = config;
    }

    /**
     * Execute code generation.
     */
    public void execute() {
        for (String pojoClass : config.getPojoClasses()) {
            try {
                generateSinglePojo(pojoClass);
            } catch (IOException | NoSuchAlgorithmException | ClassNotFoundException e) {
                log.error("Error generating code for {}: {}", pojoClass, e.getMessage(), e);
            }
        }
        log.info("所有代码生成任务完成!");
    }

    private void generateSinglePojo(String pojoClassName) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        // 1. Parse the POJO class
        SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer();
        ClassMetadata classMetadata = analyzer.parse(pojoClassName, config.getModuleName());
        log.info("Successfully parsed POJO: {}", classMetadata.getClassName());

        // 2. 创建包配置
        String basePackage = classMetadata.getPackageName().substring(0, classMetadata.getPackageName().lastIndexOf("."));
        PackageStructure packageStructure = new PackageStructure(basePackage);

        // 3. 创建文件生成器
        CodeFileWriter codeFileWriter = new CodeFileWriter(config.getOutputBaseDir());

        // 4. 定义需要生成的代码类型
        List<CodeGenerator> generators = Arrays.asList(
                new DtoGenerator(packageStructure),
                new ServiceGenerator(packageStructure),
                new ServiceImplGenerator(packageStructure),
                new RepositoryGenerator(packageStructure),
                new RequestGenerator(packageStructure),
                new QueryGenerator(packageStructure),
                new ResponseGenerator(packageStructure),
                new MapstructGenerator(packageStructure)
        );

        // 5. 生成所有代码
        for (CodeGenerator generator : generators) {
            codeFileWriter.generateFile(generator, classMetadata);
        }

        log.info("为 {} 生成的代码已完成!", classMetadata.getClassName());
        log.info("生成的文件位于: {}", new File(config.getOutputBaseDir(), "src/main/java").getAbsolutePath());
    }
}
