package io.github.youngerier.generator.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * JavaParser 符号求解器配置，全局缓存 {@link CombinedTypeSolver} 与已注册的源根，
 * 避免重复初始化以提升解析性能。
 */
@Slf4j
final class SymbolSolverSetup {

    private static CombinedTypeSolver cachedSolver;
    private static final Set<String> registeredRoots = new HashSet<>();

    private SymbolSolverSetup() {
    }

    /**
     * 注册源文件所属模块的源根，并将缓存的符号求解器设置到全局解析配置上。
     */
    static synchronized void registerSourceRoot(File sourceFile) {
        if (cachedSolver == null) {
            cachedSolver = new CombinedTypeSolver();
            cachedSolver.add(new ReflectionTypeSolver());
        }

        try {
            File sourceRoot = SourceFileLocator.findSourceRoot(sourceFile);
            if (registeredRoots.add(sourceRoot.getAbsolutePath())) {
                cachedSolver.add(new JavaParserTypeSolver(sourceRoot));
                log.debug("Registered source root: {}", sourceRoot.getAbsolutePath());
            }
        } catch (Exception e) {
            // 找不到源根或注册失败时不影响整体流程
            log.debug("Cannot register source root for file: {}", sourceFile.getAbsolutePath(), e);
        }

        StaticJavaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(cachedSolver));
    }
}
