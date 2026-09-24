package io.github.youngerier.generator.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * 源码扫描器：直接在源根下查找标注 {@code @GenModel} 的顶层类，不依赖任何编译产物。
 *
 * <p>只扫描项目自己的源根（依赖 jar 不会被当作来源），包名白名单由
 * {@link PackageMatcher} 权威判定；结果按全限定名排序，保证生成顺序可复现。
 */
@Slf4j
public final class PojoSourceScanner {

    private final PackageMatcher packageMatcher;
    private final JavaParser javaParser;

    public PojoSourceScanner(Collection<String> scanPackages, JavaParser javaParser) {
        this.packageMatcher = new PackageMatcher(scanPackages);
        this.javaParser = Objects.requireNonNull(javaParser, "javaParser cannot be null");
    }

    /**
     * 扫描全部源根并收集待生成模型；多个源根包含同一类时按全限定名去重。
     */
    public List<ModelInput> scan(Collection<Path> sourceRoots) throws IOException {
        Objects.requireNonNull(sourceRoots, "sourceRoots cannot be null");
        if (packageMatcher.isEmpty()) {
            return List.of();
        }

        NavigableMap<String, ModelInput> models = new TreeMap<>();
        for (Path sourceRoot : sourceRoots) {
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(sourceRoot)) {
                files.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> collectFromFile(path, models));
            }
        }
        return List.copyOf(models.values());
    }

    private void collectFromFile(Path file, NavigableMap<String, ModelInput> models) {
        try {
            ParseResult<CompilationUnit> result = javaParser.parse(file);
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                log.warn("源文件解析失败，跳过: {}", file);
                return;
            }
            CompilationUnit compilationUnit = result.getResult().get();
            if (!packageMatcher.matches(compilationUnit.getPackageDeclaration()
                    .map(declaration -> declaration.getNameAsString()).orElse(""))) {
                return;
            }
            // 只取顶层类型，避免内部类重复
            for (TypeDeclaration<?> topLevel : compilationUnit.getTypes()) {
                if (topLevel instanceof ClassOrInterfaceDeclaration type
                        && !type.isInterface()
                        && type.getAnnotationByName("GenModel").isPresent()) {
                    ModelInput input = new ModelInput(file, compilationUnit, type);
                    models.putIfAbsent(input.qualifiedName(), input);
                }
            }
        } catch (IOException e) {
            log.warn("读取源文件失败，跳过: {}", file, e);
        }
    }
}
