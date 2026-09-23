package com.acme.entity;

import io.github.youngerier.generator.annotation.GenModel;

/**
 * 测试夹具：位于期望被扫描的包内。
 *
 * <p>仅是扫描过滤的样本，不参与代码生成断言。
 */
@GenModel
public class SampleEntity {

    private Long id;

    private String name;
}
