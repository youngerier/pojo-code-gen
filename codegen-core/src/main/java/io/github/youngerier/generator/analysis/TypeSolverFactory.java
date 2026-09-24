package io.github.youngerier.generator.analysis;

import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.nio.file.Path;
import java.util.Collection;

/**
 * 符号求解器工厂：按「项目源码 → 项目依赖 → JDK」的顺序组合类型求解器。
 */
public final class TypeSolverFactory {

    private TypeSolverFactory() {
    }

    /**
     * @param sourceRoots           项目自身的源码根目录（实体与同包类型从源码解析）
     * @param dependencyClassLoader 覆盖项目编译期依赖的类加载器，为 null 时跳过；
     *                              仅靠 {@link ReflectionTypeSolver}（默认只含 JRE）无法解析
     *                              第三方库中的字段类型
     */
    public static CombinedTypeSolver combined(Collection<Path> sourceRoots,
                                              ClassLoader dependencyClassLoader) {
        CombinedTypeSolver combined = new CombinedTypeSolver();
        for (Path sourceRoot : sourceRoots) {
            combined.add(new JavaParserTypeSolver(sourceRoot));
        }
        if (dependencyClassLoader != null) {
            combined.add(new ClassLoaderTypeSolver(dependencyClassLoader));
        }
        combined.add(new ReflectionTypeSolver());
        return combined;
    }
}
