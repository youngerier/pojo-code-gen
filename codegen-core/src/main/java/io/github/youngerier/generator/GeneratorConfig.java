package io.github.youngerier.generator;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GeneratorConfig {

    /**
     * 当前模块的名称 (例如 "example")
     */
    private final String moduleName;

    /**
     * 生成代码的根输出目录 (例如 "target/generated-sources")
     */
    private final String outputBaseDir;

    /**
     * A list of fully qualified class names of the POJOs for which code needs to be generated.
     */
    private final List<String> pojoClasses;

}
