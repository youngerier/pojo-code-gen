package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.GeneratedType;
import io.github.youngerier.generator.model.PackageStructure;

/**
 * 生成器基类，统一持有 {@link PackageStructure}，并依据 {@link GeneratedType}
 * 统一实现生成文件的包名与类名。
 */
public abstract class BaseGenerator implements CodeGenerator {

    protected final PackageStructure packages;
    private final GeneratedType outputType;

    protected BaseGenerator(PackageStructure packages, GeneratedType outputType) {
        this.packages = packages;
        this.outputType = outputType;
    }

    @Override
    public final String getPackageName() {
        return packages.type(outputType).packageName();
    }

    @Override
    public final String getClassName() {
        return packages.type(outputType).simpleName();
    }
}
