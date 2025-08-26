package com.example.generator;

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
     * 需要生成代码的POJO文件的完整路径列表。
     */
    private final List<String> pojoPaths;

}
