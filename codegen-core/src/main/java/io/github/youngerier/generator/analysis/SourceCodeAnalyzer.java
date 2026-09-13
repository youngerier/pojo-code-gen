package io.github.youngerier.generator.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.github.youngerier.generator.model.ClassMetadata;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * 源码分析器，使用 JavaParser 解析 Java 源码并提取类元数据信息。
 *
 * <p>只负责解析编排：源文件定位委托 {@link SourceFileLocator}，符号求解委托
 * {@link SymbolSolverSetup}，字段与注释提取委托 {@link FieldExtractor}。
 * 字段提取会额外包含直接父类（非 Object）一层的实例字段。
 */
@Slf4j
public class SourceCodeAnalyzer {

    private final SourceFileLocator sourceFileLocator = new SourceFileLocator();

    /**
     * 解析 POJO 类并提取元数据信息。
     *
     * @param clazz      要解析的 Class 对象，不能为 null
     * @param moduleName 模块名称，可以为 null 或空字符串
     * @return 解析后的类元数据信息
     * @throws IOException 如果发生 I/O 错误或找不到源文件
     */
    public ClassMetadata parse(Class<?> clazz, String moduleName) throws IOException {
        Objects.requireNonNull(clazz, "Class cannot be null");
        Objects.requireNonNull(clazz.getPackage(), "Class package cannot be null");

        try {
            File sourceFile = sourceFileLocator.locate(clazz, moduleName);
            log.debug("Found source file for class {}: {}", clazz.getName(), sourceFile.getAbsolutePath());
            SymbolSolverSetup.registerSourceRoot(sourceFile);

            CompilationUnit compilationUnit = StaticJavaParser.parse(sourceFile);
            ClassMetadata metadata = new ClassMetadata();
            metadata.setPackageName(clazz.getPackage().getName());

            compilationUnit.getClassByName(clazz.getSimpleName()).ifPresent(cls -> {
                metadata.setClassName(cls.getNameAsString());
                metadata.setClassComment(Comments.extract(cls));
                FieldExtractor.extract(cls, metadata);
                addDirectParentFields(cls, moduleName, metadata);
            });
            return metadata;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to parse class: " + clazz.getName(), e);
        }
    }

    /**
     * 解析直接父类（仅一层）的实例字段，父类找不到或是 Object 时忽略。
     */
    private void addDirectParentFields(ClassOrInterfaceDeclaration cls, String moduleName, ClassMetadata metadata) {
        cls.getExtendedTypes().stream().findFirst().ifPresent(extendedType -> {
            String parentQualifiedName = resolveParentQualifiedName(extendedType);
            if (parentQualifiedName == null || isObjectClass(parentQualifiedName)) {
                return;
            }
            try {
                Class<?> parentClass = Class.forName(parentQualifiedName);
                File parentSourceFile = sourceFileLocator.locate(parentClass, moduleName);
                StaticJavaParser.parse(parentSourceFile)
                        .getClassByName(parentClass.getSimpleName())
                        .ifPresent(parentCls -> FieldExtractor.extract(parentCls, metadata));
            } catch (Exception e) {
                // 父类解析失败时忽略，不影响当前类解析
                log.debug("Failed to parse parent class fields for: {}", parentQualifiedName, e);
            }
        });
    }

    private boolean isObjectClass(String qualifiedName) {
        return "java.lang.Object".equals(qualifiedName) || "Object".equals(qualifiedName);
    }

    private String resolveParentQualifiedName(ClassOrInterfaceType extendedType) {
        try {
            return extendedType.resolve().asReferenceType().getQualifiedName();
        } catch (Exception e) {
            return extendedType.getNameAsString();
        }
    }
}
