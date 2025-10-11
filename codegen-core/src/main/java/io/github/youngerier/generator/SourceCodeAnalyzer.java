package io.github.youngerier.generator;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.squareup.javapoet.ClassName;
import io.github.youngerier.generator.model.ClassMetadata;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 源码分析器，使用JavaParser解析Java源码并提取类元数据信息
 * 重构版本：使用 Reflections 库简化源文件发现，使用 NIO Path API 优化路径操作
 */
@Slf4j
public class SourceCodeAnalyzer {

    private static final String SRC_MAIN_JAVA = "src" + File.separator + "main" + File.separator + "java";
    private static final String SRC_TEST_JAVA = "src" + File.separator + "test" + File.separator + "java";

    // 缓存符号求解器与已注册的源根，避免重复初始化与提升解析性能
    private static CombinedTypeSolver CACHED_SOLVER;
    private static final Set<String> REGISTERED_ROOTS = new HashSet<>();

    /**
     * Parse a POJO class using Class object.
     *
     * @param clazz The Class object of the POJO.
     * @return The parsed POJO information.
     * @throws IOException If an I/O error occurs.
     */
    public ClassMetadata parse(Class<?> clazz, String moduleName) throws IOException {
        // Find the source file using improved logic
        File sourceFile = findSourceFileOptimized(clazz, moduleName);

        // Configure symbol solver
        configureSymbolSolver(sourceFile);

        // Parse the source file
        CompilationUnit cu = StaticJavaParser.parse(sourceFile);
        String simpleClassName = clazz.getSimpleName();

        ClassMetadata classMetadata = new ClassMetadata();
        classMetadata.setPackageName(clazz.getPackage().getName());

        // Extract class information
        cu.getClassByName(simpleClassName).ifPresent(cls -> {
            classMetadata.setClassName(cls.getNameAsString());
            classMetadata.setClassComment(extractComment(cls));
            extractFields(cls, classMetadata);
            // 解析直接父类字段（仅一层），如果父类是 Object 则跳过
            addDirectParentFields(cls, moduleName, classMetadata);
        });

        return classMetadata;
    }

    /**
     * 优化的源文件查找方法，使用 Reflections 和 NIO Path API
     */
    private File findSourceFileOptimized(Class<?> clazz, String moduleName) throws IOException {
        try {
            // 首先尝试从 classpath 获取
            File sourceFile = getSourceFileFromClasspathOptimized(clazz);
            if (sourceFile != null && sourceFile.exists()) {
                return sourceFile;
            }

            // 使用 Reflections 进行智能搜索
            return findSourceFileUsingReflections(clazz, moduleName);
        } catch (Exception e) {
            throw new IOException("Failed to find source file for class: " + clazz.getName(), e);
        }
    }

    /**
     * 使用优化的 NIO Path API 从 classpath 获取源文件
     */
    private File getSourceFileFromClasspathOptimized(Class<?> clazz) throws URISyntaxException {
        URL resource = clazz.getResource(clazz.getSimpleName() + ".class");
        if (resource != null) {
            Path classPath = Paths.get(resource.toURI());
            Path sourcePath = convertClassPathToSourcePathOptimized(classPath, clazz.getSimpleName());
            if (sourcePath != null && Files.exists(sourcePath)) {
                return sourcePath.toFile();
            }
        }
        return null;
    }

    /**
     * 使用 NIO Path API 优化的路径转换
     */
    private Path convertClassPathToSourcePathOptimized(Path classPath, String className) {
        String classPathStr = classPath.toString();
        
        // 定义可能的路径转换规则
        String[][] pathMappings = {
            {"/target/classes", "/" + SRC_MAIN_JAVA},
            {"/target/test-classes", "/" + SRC_TEST_JAVA},
            {"/build/classes/java/main", "/" + SRC_MAIN_JAVA},
            {"/build/classes/java/test", "/" + SRC_TEST_JAVA},
            {"/out/production/classes", "/" + SRC_MAIN_JAVA},
            {"/out/test/classes", "/" + SRC_TEST_JAVA}
        };

        for (String[] mapping : pathMappings) {
            if (classPathStr.contains(mapping[0])) {
                String sourcePathStr = classPathStr
                    .replace(mapping[0], mapping[1])
                    .replace(className + ".class", className + ".java");
                return Paths.get(sourcePathStr);
            }
        }
        return null;
    }

    /**
     * 使用 Reflections 库查找源文件
     */
    private File findSourceFileUsingReflections(Class<?> clazz, String moduleName) throws IOException {
        String packageName = clazz.getPackage().getName();
        String className = clazz.getSimpleName();
        
        // 获取项目根目录
        Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        
        // 优先搜索指定模块
        if (moduleName != null && !moduleName.isEmpty()) {
            Path moduleDir = projectRoot.resolve(moduleName);
            if (Files.exists(moduleDir)) {
                File sourceFile = searchSourceFileInModule(moduleDir, packageName, className);
                if (sourceFile != null) {
                    return sourceFile;
                }
            }
        }

        // 搜索整个项目
        return searchSourceFileInProject(projectRoot, packageName, className);
    }

    /**
     * 在指定模块中搜索源文件
     */
    private File searchSourceFileInModule(Path moduleDir, String packageName, String className) {
        String packagePath = packageName.replace('.', File.separatorChar);
        String fileName = className + ".java";

        // 搜索 src/main/java 和 src/test/java
        Path[] sourceDirs = {
            moduleDir.resolve(SRC_MAIN_JAVA),
            moduleDir.resolve(SRC_TEST_JAVA)
        };

        for (Path sourceDir : sourceDirs) {
            if (Files.exists(sourceDir)) {
                Path sourceFile = sourceDir.resolve(packagePath).resolve(fileName);
                if (Files.exists(sourceFile)) {
                    return sourceFile.toFile();
                }
            }
        }
        return null;
    }

    /**
     * 在整个项目中搜索源文件
     */
    private File searchSourceFileInProject(Path projectRoot, String packageName, String className) throws IOException {
        String packagePath = packageName.replace('.', File.separatorChar);
        String fileName = className + ".java";

        // 使用 Files.walk 遍历项目目录
        try {
            return Files.walk(projectRoot, 6) // 限制深度避免过深遍历
                .filter(Files::isDirectory)
                .filter(path -> path.getFileName().toString().equals("java"))
                .filter(path -> path.toString().contains("src"))
                .map(javaDir -> javaDir.resolve(packagePath).resolve(fileName))
                .filter(Files::exists)
                .map(Path::toFile)
                .findFirst()
                .orElseThrow(() -> new IOException("Source file not found for class: " + packageName + "." + className));
        } catch (IOException e) {
            throw new IOException("Error searching for source file: " + packageName + "." + className, e);
        }
    }

    // 提取直接父类的字段（仅一层），找不到或是 Object 则忽略
    private void addDirectParentFields(ClassOrInterfaceDeclaration cls, String moduleName, ClassMetadata classMetadata) {
        cls.getExtendedTypes().stream().findFirst().ifPresent(ext -> {
            String parentQualifiedName = resolveParentQualifiedName(ext);
            if (parentQualifiedName == null) {
                return;
            }
            if ("java.lang.Object".equals(parentQualifiedName) || "Object".equals(parentQualifiedName)) {
                return;
            }
            try {
                Class<?> parentClass = Class.forName(parentQualifiedName);
                File parentSourceFile = findSourceFileOptimized(parentClass, moduleName);
                CompilationUnit parentCu = StaticJavaParser.parse(parentSourceFile);
                parentCu.getClassByName(parentClass.getSimpleName()).ifPresent(parentCls -> extractFields(parentCls, classMetadata));
            } catch (Exception ignore) {
                // 父类解析失败时忽略，不影响当前类解析
            }
        });
    }

    // 将扩展类型解析为父类全限定名，失败时返回简单名或 null
    private String resolveParentQualifiedName(ClassOrInterfaceType ext) {
        try {
            return ext.resolve().asReferenceType().getQualifiedName();
        } catch (Exception e) {
            try {
                return ext.getNameAsString();
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    /**
     * Configure the symbol solver for JavaParser.
     *
     * @param sourceFile The source file to configure symbol solver for.
     */
    private void configureSymbolSolver(File sourceFile) {
        synchronized (SourceCodeAnalyzer.class) {
            if (CACHED_SOLVER == null) {
                CACHED_SOLVER = new CombinedTypeSolver();
                CACHED_SOLVER.add(new ReflectionTypeSolver());
            }
            // 收集并注册源根（支持多模块 src/main/java 与 src/test/java）
            for (File root : collectSourceRoots(sourceFile)) {
                String path = root.getAbsolutePath();
                if (REGISTERED_ROOTS.add(path)) {
                    try {
                        CACHED_SOLVER.add(new JavaParserTypeSolver(root));
                        // 注册成功，避免重复日志依赖
                    } catch (Exception e) {
                        // 注册失败不影响整体流程，安全忽略
                    }
                }
            }
            // 统一设置符号解析器（指向缓存的 solver）
            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(CACHED_SOLVER);
            StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
        }
    }

    // 收集项目中的源根目录（当前源文件所在模块，以及工作空间所有包含 pom.xml 的模块）
    private List<File> collectSourceRoots(File sourceFile) {
        List<File> roots = new ArrayList<>();
        try {
            File srcMainJava = findSrcMainJavaDir(sourceFile);
            if (srcMainJava.exists()) {
                roots.add(srcMainJava);
            }
        } catch (Exception e) {
            // 找不到当前模块源根时忽略，继续收集工作空间其他模块
        }
        return roots;
    }

    /**
     * Find the src/main/java directory for a source file.
     *
     * @param sourceFile The source file.
     * @return The src/main/java directory.
     */
    private File findSrcMainJavaDir(File sourceFile) {
        Path sourcePath = sourceFile.toPath().toAbsolutePath().normalize();
        Path current = sourcePath.getParent();

        // Walk up the directory tree to find src/main/java
        while (current != null) {
            Path srcMainJava = current.resolve(SRC_MAIN_JAVA);
            if (srcMainJava.toFile().exists()) {
                return srcMainJava.toFile();
            }
            current = current.getParent();
        }

        throw new IllegalStateException("Cannot find src/main/java directory for source file: " + sourceFile.getAbsolutePath());
    }

    /**
     * Extract field information using visitor pattern for better performance.
     */
    private void extractFields(ClassOrInterfaceDeclaration cls, ClassMetadata classMetadata) {
        // 使用 visitor 模式遍历字段
        cls.accept(new FieldVisitor(), classMetadata);
    }

    /**
     * 字段访问者，使用 visitor 模式优化字段提取
     */
    private static class FieldVisitor extends com.github.javaparser.ast.visitor.VoidVisitorAdapter<ClassMetadata> {
        @Override
        public void visit(FieldDeclaration fieldDecl, ClassMetadata classMetadata) {
            // Skip static fields
            if (fieldDecl.isStatic()) {
                return;
            }

            for (VariableDeclarator var : fieldDecl.getVariables()) {
                ClassMetadata.FieldInfo fieldInfo = new ClassMetadata.FieldInfo();
                fieldInfo.setName(var.getNameAsString());

                // Set field type
                try {
                    ResolvedType resolvedType = var.getType().resolve();
                    fieldInfo.setFullType(resolvedType.describe());
                    fieldInfo.setType(ClassName.bestGuess(resolvedType.describe()));
                } catch (Exception e) {
                    fieldInfo.setType(ClassName.bestGuess(var.getTypeAsString()));
                    fieldInfo.setFullType(var.getTypeAsString());
                }

                fieldInfo.setComment(extractComment(fieldDecl));
                fieldInfo.setPrimaryKey(isPrimaryKey(fieldInfo.getName()));

                classMetadata.getFields().add(fieldInfo);
            }
        }
    }

    /**
     * Extract comment content.
     *
     * @param node The node containing the comment.
     * @return The extracted comment content, or an empty string if there is no comment.
     */
    private static String extractComment(Node node) {
        // 优先解析 Javadoc 注释（通过判断 Comment 类型），其次处理普通块/行注释
        return node.getComment()
                .map(c -> {
                    if (c instanceof JavadocComment) {
                        return ((JavadocComment) c)
                                .parse()
                                .getDescription()
                                .toText()
                                .trim();
                    }
                    // 对普通注释逐行清洗，移除前导的 * 与多余空白，保留换行与可读性
                    String[] lines = c.getContent().split("\\R");
                    StringBuilder sb = new StringBuilder();
                    for (String line : lines) {
                        String cleaned = line.replaceFirst("^\\s*\\*+\\s?", "").trim();
                        if (!cleaned.isEmpty()) {
                            if (!sb.isEmpty()) sb.append('\n');
                            sb.append(cleaned);
                        }
                    }
                    return sb.toString();
                })
                .orElse("");
    }

    /**
     * Check if it's a primary key field.
     */
    private static boolean isPrimaryKey(String fieldName) {
        return "id".equals(fieldName) || fieldName.endsWith("Id");
    }
}