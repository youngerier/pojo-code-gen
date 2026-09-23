package com.acme.entity.sub;

import io.github.youngerier.generator.annotation.GenModel;

/**
 * 测试夹具：位于配置包之下的一级子包内，用于验证 {@code scanPackages} 覆盖子包。
 */
@GenModel
public class NestedSampleEntity {

    private Long id;
}
