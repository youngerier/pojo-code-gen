package io.github.youngerier.generator;

import io.github.youngerier.generator.analysis.ModelInput;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GeneratorConfig {

    /**
     * 生成代码的根输出目录，例如 {@code target/generated-sources/pojo-codegen}。
     * 生成产物直接写入该目录下对应包路径，不再追加 {@code src/main/java}。
     */
    private final String outputBaseDir;

    /**
     * 待生成的模型输入列表（由源码扫描器直接从 .java 源文件产出）。
     */
    private final List<ModelInput> models;
}
