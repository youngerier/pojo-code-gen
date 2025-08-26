package com.example.generator;

import com.example.generator.generators.*;
import com.example.generator.model.PackageLayout;
import com.example.generator.model.PojoInfo;
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
        PojoParser parser = new PojoParser();
        PojoInfo pojoInfo = parser.parse(pojoClassName, config.getModuleName());
        log.info("Successfully parsed POJO: {}", pojoInfo.getClassName());

        // 2. 创建包配置
        String basePackage = pojoInfo.getPackageName().substring(0, pojoInfo.getPackageName().lastIndexOf("."));
        PackageLayout packageLayout = new PackageLayout(basePackage);

        // 3. 创建文件生成器
        FileGenerator fileGenerator = new FileGenerator(config.getOutputBaseDir());

        // 4. 定义需要生成的代码类型
        List<CodeGenerator> generators = Arrays.asList(
                new DtoGenerator(packageLayout),
                new ServiceGenerator(packageLayout),
                new ServiceImplGenerator(packageLayout),
                new RepositoryGenerator(packageLayout),
                new RequestGenerator(packageLayout),
                new QueryGenerator(packageLayout),
                new ResponseGenerator(packageLayout),
                new MapstructGenerator(packageLayout)
        );

        // 5. 生成所有代码
        for (CodeGenerator generator : generators) {
            fileGenerator.generateFile(generator, pojoInfo);
        }

        log.info("为 {} 生成的代码已完成!", pojoInfo.getClassName());
        log.info("生成的文件位于: {}", new File(config.getOutputBaseDir(), "src/main/java").getAbsolutePath());
    }
}
