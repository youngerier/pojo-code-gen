package com.example.generator;

import com.example.generator.generators.*;
import com.example.generator.generators.DtoGenerator;
import com.example.generator.generators.ServiceGenerator;
import com.example.generator.generators.ServiceImplGenerator;
import com.example.generator.generators.RepositoryGenerator;
import com.example.generator.generators.RequestGenerator;
import com.example.generator.generators.ResponseGenerator;
import com.example.generator.generators.MapstructGenerator;
import com.example.generator.model.PojoInfo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 代码生成器主程序
 */
public class CodeGeneratorMain {

    public static void codeGen(String pojoFilePath) {

        // 处理POJO类路径，支持类名格式（如com.abc.entity.User）
        if (!pojoFilePath.endsWith(".java")) {
            // 将类名格式转换为文件路径
            pojoFilePath = pojoFilePath.replace('.', File.separatorChar) + ".java";
            // 检查是否是相对路径，如果是则添加项目根路径
            if (!new File(pojoFilePath).isAbsolute()) {
                pojoFilePath = new File(System.getProperty("user.dir"), "src/main/java/" + pojoFilePath).getAbsolutePath();
            }
        }

        // 验证POJO文件是否存在
        File pojoFile = new File(pojoFilePath);
        if (!pojoFile.exists() || !pojoFile.isFile()) {
            System.err.println("错误: POJO文件不存在 - " + pojoFilePath);
            return;
        }

        // 处理输出目录，默认为target/code-gen
        String outputDir = "target/code-gen";

        try {
            // 1. 解析POJO文件
            PojoParser parser = new PojoParser();
            PojoInfo pojoInfo = parser.parse(pojoFilePath);
            System.out.println("成功解析POJO: " + pojoInfo.getClassName());

            // 确保输出目录存在
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists()) {
                outputDirFile.mkdirs();
                System.out.println("创建输出目录: " + outputDirFile.getAbsolutePath());
            }

            // 2. 创建文件生成器
            FileGenerator fileGenerator = new FileGenerator(outputDir);

            // 3. 定义需要生成的代码类型
            List<CodeGenerator> generators = Arrays.asList(
                    new DtoGenerator(),
                    new ServiceGenerator(),
                    new ServiceImplGenerator(),
                    new RepositoryGenerator(),
                    new RequestGenerator(),
                    new ResponseGenerator(),
                    new MapstructGenerator()
            );

            // 4. 生成所有代码
            for (CodeGenerator generator : generators) {
                fileGenerator.generateFile(generator, pojoInfo);
            }

            System.out.println("代码生成完成!");
            System.out.println("生成的文件位置: " + new File(outputDir, "src/main/java").getAbsolutePath());

        } catch (IOException e) {
            System.err.println("代码生成失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        codeGen("com.abc.entity.User");
    }
}
