package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.GeneratedType;
import io.github.youngerier.generator.model.PackageStructure;

/**
 * Response 模型类生成器
 */
public class ResponseGenerator extends AbstractModelGenerator {

    public ResponseGenerator(PackageStructure packageStructure) {
        super(packageStructure, GeneratedType.RESPONSE, "响应对象");
    }
}
