package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.PackageStructure;

import java.util.List;
import java.util.function.Function;

/**
 * 代码生成器清单，按固定顺序为一个实体创建全部生成器。
 */
public final class Generators {

    private static final List<Function<PackageStructure, CodeGenerator>> FACTORIES = List.of(
            DtoGenerator::new,
            ServiceGenerator::new,
            ServiceImplGenerator::new,
            MapperGenerator::new,
            ControllerGenerator::new,
            RequestGenerator::new,
            QueryGenerator::new,
            ResponseGenerator::new,
            MapstructGenerator::new,
            RepositoryGenerator::new);

    private Generators() {
    }

    public static List<CodeGenerator> createAll(PackageStructure packageStructure) {
        return FACTORIES.stream()
                .map(factory -> factory.apply(packageStructure))
                .toList();
    }
}
