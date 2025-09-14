package io.github.youngerier.generator;

import io.github.youngerier.generator.generators.ControllerGenerator;
import io.github.youngerier.generator.generators.DtoGenerator;
import io.github.youngerier.generator.generators.MapstructGenerator;
import io.github.youngerier.generator.generators.QueryGenerator;
import io.github.youngerier.generator.generators.RepositoryGenerator;
import io.github.youngerier.generator.generators.RequestGenerator;
import io.github.youngerier.generator.generators.ResponseGenerator;
import io.github.youngerier.generator.generators.ServiceGenerator;
import io.github.youngerier.generator.generators.ServiceImplGenerator;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.PackageStructure;
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

    /**
     * Standard Maven source directory path.
     */
    private static final String SRC_MAIN_JAVA = "src" + File.separator + "main" + File.separator + "java";

    private final GeneratorConfig config;

    public GeneratorEngine(GeneratorConfig config) {
        this.config = config;
    }

    /**
     * Execute code generation.
     */
    public void execute() {
        for (Class<?> pojoClass : config.getPojoClasses()) {
            try {
                generateSinglePojo(pojoClass);
            } catch (IOException | NoSuchAlgorithmException e) {
                log.error("Error generating code for {}: {}", pojoClass.getName(), e.getMessage(), e);
            }
        }
        log.info("所有代码生成任务完成!");
    }

    private void generateSinglePojo(Class<?> pojoClass) throws IOException, NoSuchAlgorithmException {
        // 1. Parse the POJO class
        SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer();
        ClassMetadata classMetadata = analyzer.parse(pojoClass);
        log.info("Successfully parsed POJO: {}", classMetadata.getClassName());

        // 2. 创建包配置
        String basePackage = classMetadata.getBasePackageName();
        PackageStructure packageStructure = new PackageStructure(basePackage, classMetadata.getClassName());

        // 3. 创建文件生成器
        CodeFileWriter codeFileWriter = new CodeFileWriter(config.getOutputBaseDir());

        // 4. 定义需要生成的代码类型
        List<CodeGenerator> generators = Arrays.asList(
                new DtoGenerator(packageStructure),
                new ServiceGenerator(packageStructure),
                new ServiceImplGenerator(packageStructure),
                new RepositoryGenerator(packageStructure),
                new ControllerGenerator(packageStructure),
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
        log.info("生成的文件位于: {}", new File(config.getOutputBaseDir(), SRC_MAIN_JAVA).getAbsolutePath());
    }
}