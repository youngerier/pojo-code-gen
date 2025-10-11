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
import java.util.Objects;
import java.util.Set;

/**
 * 源码分析器，使用 JavaParser 解析 Java 源码并提取类元数据信息。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>智能源文件发现：支持多种构建工具路径映射（Maven、Gradle、IntelliJ IDEA）</li>
 *   <li>高效字段提取：使用 Visitor 模式优化 AST 遍历性能</li>
 *   <li>符号解析缓存：避免重复初始化，提升解析性能</li>
 *   <li>多模块支持：支持复杂项目结构的源文件定位</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>{@code
 * SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer();
 * ClassMetadata metadata = analyzer.parse(MyClass.class, "my-module");
 * }</pre>
 * 
 * @author Generated
 * @since 1.0.0
 */
@Slf4j
public class SourceCodeAnalyzer {

    private static final String SRC_MAIN_JAVA = "src" + File.separator + "main" + File.separator + "java";
    private static final String SRC_TEST_JAVA = "src" + File.separator + "test" + File.separator + "java";
    private static final int MAX_SEARCH_DEPTH = 6;

    // 缓存符号求解器与已注册的源根，避免重复初始化与提升解析性能
    private static CombinedTypeSolver cachedSolver;
    private static final Set<String> registeredRoots = new HashSet<>();

    /**
     * 解析 POJO 类并提取元数据信息。
     *
     * @param clazz      要解析的 Class 对象，不能为 null
     * @param moduleName 模块名称，可以为 null 或空字符串
     * @return 解析后的类元数据信息
     * @throws IllegalArgumentException 如果 clazz 为 null
     * @throws IOException             如果发生 I/O 错误或找不到源文件
     */
    public ClassMetadata parse(Class<?> clazz, String moduleName) throws IOException {
        validateInput(clazz);
        
        try {
            // 查找源文件
            File sourceFile = findSourceFile(clazz, moduleName);
            log.debug("Found source file for class {}: {}", clazz.getName(), sourceFile.getAbsolutePath());

            // 配置符号求解器
            configureSymbolSolver(sourceFile);

            // 解析源文件
            CompilationUnit compilationUnit = StaticJavaParser.parse(sourceFile);
            String simpleClassName = clazz.getSimpleName();

            ClassMetadata classMetadata = new ClassMetadata();
            classMetadata.setPackageName(clazz.getPackage().getName());

            // 提取类信息
            compilationUnit.getClassByName(simpleClassName).ifPresent(cls -> {
                classMetadata.setClassName(cls.getNameAsString());
                classMetadata.setClassComment(extractComment(cls));
                extractFields(cls, classMetadata);
                // 解析直接父类字段（仅一层），如果父类是 Object 则跳过
                addDirectParentFields(cls, moduleName, classMetadata);
            });

            return classMetadata;
        } catch (Exception e) {
            throw new IOException("Failed to parse class: " + clazz.getName(), e);
        }
    }

    /**
     * 验证输入参数的有效性。
     */
    private void validateInput(Class<?> clazz) {
        Objects.requireNonNull(clazz, "Class cannot be null");
        Objects.requireNonNull(clazz.getPackage(), "Class package cannot be null");
    }

    /**
     * 智能源文件查找，支持多种构建工具和项目结构。
     */
    private File findSourceFile(Class<?> clazz, String moduleName) throws IOException {
        try {
            // 首先尝试从 classpath 获取
            File sourceFile = getSourceFileFromClasspath(clazz);
            if (sourceFile != null && sourceFile.exists()) {
                return sourceFile;
            }

            // 使用项目结构搜索
            return findSourceFileInProject(clazz, moduleName);
        } catch (Exception e) {
            throw new IOException("Cannot locate source file for class: " + clazz.getName(), e);
        }
    }

    /**
     * 从 classpath 获取源文件，使用 NIO Path API 优化路径操作。
     */
    private File getSourceFileFromClasspath(Class<?> clazz) {
        try {
            URL resource = clazz.getResource(clazz.getSimpleName() + ".class");
            if (resource != null) {
                Path classPath = Paths.get(resource.toURI());
                Path sourcePath = convertClassPathToSourcePath(classPath, clazz.getSimpleName());
                if (sourcePath != null && Files.exists(sourcePath)) {
                    return sourcePath.toFile();
                }
            }
        } catch (URISyntaxException e) {
            log.debug("Failed to convert class resource to source path for class: {}", clazz.getName(), e);
        }
        return null;
    }

    /**
     * 将 class 文件路径转换为对应的源文件路径。
     * 支持多种构建工具的路径映射规则。
     */
    private Path convertClassPathToSourcePath(Path classPath, String className) {
        String classPathStr = classPath.toString();
        
        // 定义可能的路径转换规则
        String[][] pathMappings = {
            {"/target/classes", "/" + SRC_MAIN_JAVA},           // Maven
            {"/target/test-classes", "/" + SRC_TEST_JAVA},     // Maven Test
            {"/build/classes/java/main", "/" + SRC_MAIN_JAVA}, // Gradle
            {"/build/classes/java/test", "/" + SRC_TEST_JAVA}, // Gradle Test
            {"/out/production/classes", "/" + SRC_MAIN_JAVA},  // IntelliJ IDEA
            {"/out/test/classes", "/" + SRC_TEST_JAVA}         // IntelliJ IDEA Test
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
     * 在项目中搜索源文件。
     */
    private File findSourceFileInProject(Class<?> clazz, String moduleName) throws IOException {
        String packageName = clazz.getPackage().getName();
        String className = clazz.getSimpleName();
        
        // 获取项目根目录
        Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        
        // 优先搜索指定模块
        if (moduleName != null && !moduleName.trim().isEmpty()) {
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
     * 在指定模块中搜索源文件。
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
     * 在整个项目中搜索源文件。
     */
    private File searchSourceFileInProject(Path projectRoot, String packageName, String className) throws IOException {
        String packagePath = packageName.replace('.', File.separatorChar);
        String fileName = className + ".java";

        // 使用 Files.walk 遍历项目目录
        try {
            return Files.walk(projectRoot, MAX_SEARCH_DEPTH) // 限制深度避免过深遍历
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

    /**
     * 提取直接父类的字段（仅一层），找不到或是 Object 则忽略。
     */
    private void addDirectParentFields(ClassOrInterfaceDeclaration cls, String moduleName, ClassMetadata classMetadata) {
        cls.getExtendedTypes().stream().findFirst().ifPresent(ext -> {
            String parentQualifiedName = resolveParentQualifiedName(ext);
            if (parentQualifiedName == null || isObjectClass(parentQualifiedName)) {
                return;
            }
            
            try {
                Class<?> parentClass = Class.forName(parentQualifiedName);
                File parentSourceFile = findSourceFile(parentClass, moduleName);
                CompilationUnit parentCu = StaticJavaParser.parse(parentSourceFile);
                parentCu.getClassByName(parentClass.getSimpleName())
                    .ifPresent(parentCls -> extractFields(parentCls, classMetadata));
            } catch (Exception e) {
                log.debug("Failed to parse parent class fields for: {}", parentQualifiedName, e);
                // 父类解析失败时忽略，不影响当前类解析
            }
        });
    }

    /**
     * 检查是否为 Object 类。
     */
    private boolean isObjectClass(String qualifiedName) {
        return "java.lang.Object".equals(qualifiedName) || "Object".equals(qualifiedName);
    }

    /**
     * 将扩展类型解析为父类全限定名，失败时返回简单名或 null。
     */
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
     * 配置 JavaParser 的符号求解器，支持缓存以提升性能。
     *
     * @param sourceFile 源文件，用于确定源根目录
     */
    private void configureSymbolSolver(File sourceFile) {
        synchronized (SourceCodeAnalyzer.class) {
            if (cachedSolver == null) {
                cachedSolver = new CombinedTypeSolver();
                cachedSolver.add(new ReflectionTypeSolver());
            }
            
            // 收集并注册源根（支持多模块 src/main/java 与 src/test/java）
            for (File root : collectSourceRoots(sourceFile)) {
                String path = root.getAbsolutePath();
                if (registeredRoots.add(path)) {
                    try {
                        cachedSolver.add(new JavaParserTypeSolver(root));
                        log.debug("Registered source root: {}", path);
                    } catch (Exception e) {
                        log.debug("Failed to register source root: {}", path, e);
                        // 注册失败不影响整体流程，安全忽略
                    }
                }
            }
            
            // 统一设置符号解析器（指向缓存的 solver）
            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(cachedSolver);
            StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
        }
    }

    /**
     * 收集项目中的源根目录。
     */
    private List<File> collectSourceRoots(File sourceFile) {
        List<File> roots = new ArrayList<>();
        try {
            File srcMainJava = findSrcMainJavaDir(sourceFile);
            if (srcMainJava.exists()) {
                roots.add(srcMainJava);
            }
        } catch (Exception e) {
            log.debug("Cannot find source root for file: {}", sourceFile.getAbsolutePath(), e);
            // 找不到当前模块源根时忽略，继续收集工作空间其他模块
        }
        return roots;
    }

    /**
     * 查找源文件对应的 src/main/java 目录。
     *
     * @param sourceFile 源文件
     * @return src/main/java 目录
     * @throws IllegalStateException 如果找不到 src/main/java 目录
     */
    private File findSrcMainJavaDir(File sourceFile) {
        Path sourcePath = sourceFile.toPath().toAbsolutePath().normalize();
        Path current = sourcePath.getParent();

        // 向上遍历目录树查找 src/main/java
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
     * 使用 Visitor 模式提取字段信息，提升性能。
     */
    private void extractFields(ClassOrInterfaceDeclaration cls, ClassMetadata classMetadata) {
        cls.accept(new FieldVisitor(), classMetadata);
    }

    /**
     * 字段访问者，使用 Visitor 模式优化字段提取性能。
     */
    private static class FieldVisitor extends com.github.javaparser.ast.visitor.VoidVisitorAdapter<ClassMetadata> {
        @Override
        public void visit(FieldDeclaration fieldDecl, ClassMetadata classMetadata) {
            // 跳过静态字段
            if (fieldDecl.isStatic()) {
                return;
            }

            for (VariableDeclarator var : fieldDecl.getVariables()) {
                ClassMetadata.FieldInfo fieldInfo = new ClassMetadata.FieldInfo();
                fieldInfo.setName(var.getNameAsString());

                // 设置字段类型
                try {
                    ResolvedType resolvedType = var.getType().resolve();
                    fieldInfo.setFullType(resolvedType.describe());
                    fieldInfo.setType(ClassName.bestGuess(resolvedType.describe()));
                } catch (Exception e) {
                    // 类型解析失败时使用原始类型字符串
                    String typeString = var.getTypeAsString();
                    fieldInfo.setType(ClassName.bestGuess(typeString));
                    fieldInfo.setFullType(typeString);
                }

                fieldInfo.setComment(extractComment(fieldDecl));
                fieldInfo.setPrimaryKey(isPrimaryKey(fieldInfo.getName()));

                classMetadata.getFields().add(fieldInfo);
            }
        }
    }

    /**
     * 提取注释内容，支持 Javadoc 和普通注释。
     *
     * @param node 包含注释的节点
     * @return 提取的注释内容，如果没有注释则返回空字符串
     */
    private static String extractComment(Node node) {
        return node.getComment()
                .map(comment -> {
                    if (comment instanceof JavadocComment) {
                        // 解析 Javadoc 注释
                        return ((JavadocComment) comment)
                                .parse()
                                .getDescription()
                                .toText()
                                .trim();
                    }
                    
                    // 处理普通注释，清理格式
                    String[] lines = comment.getContent().split("\\R");
                    StringBuilder sb = new StringBuilder();
                    for (String line : lines) {
                        String cleaned = line.replaceFirst("^\\s*\\*+\\s?", "").trim();
                        if (!cleaned.isEmpty()) {
                            if (!sb.isEmpty()) {
                                sb.append('\n');
                            }
                            sb.append(cleaned);
                        }
                    }
                    return sb.toString();
                })
                .orElse("");
    }

    /**
     * 检查字段是否为主键字段。
     * 
     * @param fieldName 字段名称
     * @return 如果是主键字段返回 true，否则返回 false
     */
    private static boolean isPrimaryKey(String fieldName) {
        return "id".equals(fieldName) || fieldName.endsWith("Id");
    }
}