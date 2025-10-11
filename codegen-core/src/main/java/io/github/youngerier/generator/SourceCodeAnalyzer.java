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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 源码分析器，使用JavaParser解析Java源码并提取类元数据信息
 */
@Slf4j
public class SourceCodeAnalyzer {

    private static final String SRC_MAIN_JAVA = "src" + File.separator + "main" + File.separator + "java";

    private static final String SRC_TEST_JAVA = "src" + File.separator + "test" + File.separator + "java";

    private static final List<File> PROJECT_SOURCE_ROOTS = new ArrayList<>();

    private static final AtomicBoolean PROJECT_ROOTS_SCANNED = new AtomicBoolean(false);

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
        // Find the source file
        File sourceFile = findSourceFile(moduleName, clazz);

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
                File parentSourceFile = findSourceFile(moduleName, parentClass);
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
     * Find the source file for a class.
     *
     * @param clazz The class to find the source file for.
     * @return The source file.
     * @throws IOException If the source file cannot be found.
     */
    private File findSourceFile(String moduleName, Class<?> clazz) throws IOException {
        try {
            // Try to get source file from classpath
            File sourceFile = getSourceFileFromClasspath(clazz);
            if (sourceFile != null && sourceFile.exists()) {
                return sourceFile;
            }

            // Fallback to package structure search
            return getSourceFileFromPackageStructure(moduleName, clazz);
        } catch (Exception e) {
            throw new IOException("Failed to find source file for class: " + clazz.getName(), e);
        }
    }

    /**
     * Get source file from classpath.
     *
     * @param clazz The class to find the source file for.
     * @return The source file, or null if not found.
     * @throws URISyntaxException If there is an error with the URI.
     */
    private File getSourceFileFromClasspath(Class<?> clazz) throws URISyntaxException {
        URL resource = clazz.getResource(clazz.getSimpleName() + ".class");
        if (resource != null) {
            String classPath = Paths.get(resource.toURI()).toString();
            String sourcePath = convertClassPathToSourcePath(classPath, clazz.getSimpleName());
            return new File(sourcePath);
        }
        return null;
    }

    /**
     * Convert class path to source path.
     *
     * @param classPath The class path.
     * @param className The class name.
     * @return The source path.
     */
    private String convertClassPathToSourcePath(String classPath, String className) {
        return classPath
                .replaceAll("[/\\\\]target[/\\\\]classes", File.separator + SRC_MAIN_JAVA)
                .replaceAll("[/\\\\]build[/\\\\]classes[/\\\\]java[/\\\\]main", File.separator + SRC_MAIN_JAVA)
                .replaceAll(className + "\\.class$", className + ".java");
    }

    /**
     * Get source file from package structure.
     *
     * @param clazz The class to find the source file for.
     * @return The source file.
     * @throws IOException If the source file cannot be found.
     */
    private File getSourceFileFromPackageStructure(String moduleName, Class<?> clazz) throws IOException {
        // 首先尝试从ProtectionDomain获取源文件位置
        File sourceFile = tryGetSourceFromProtectionDomain(clazz);
        if (sourceFile != null && sourceFile.exists()) {
            return sourceFile;
        }

        // 回退到项目搜索
        return findSourceInProject(moduleName, clazz);
    }

    /**
     * 尝试从ProtectionDomain获取源文件
     */
    private File tryGetSourceFromProtectionDomain(Class<?> clazz) {
        try {
            java.security.ProtectionDomain protectionDomain = clazz.getProtectionDomain();
            if (protectionDomain == null) {
                return null;
            }

            java.security.CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return null;
            }

            String classLocation = codeSource.getLocation().getPath();

            // JAR包情况直接返回null，让调用方使用项目搜索
            if (classLocation.endsWith(".jar")) {
                return null;
            }

            // 目录情况：尝试构建源文件路径
            return buildSourceFileFromClassLocation(classLocation, clazz);

        } catch (SecurityException | IllegalArgumentException e) {
            // 记录具体异常但不抛出，让调用方使用备选方案
            log.debug("Failed to get source from ProtectionDomain for class {}: {}",
                    clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 从类文件位置构建源文件路径
     */
    private File buildSourceFileFromClassLocation(String classLocation, Class<?> clazz) {
        if (clazz.getPackage() == null) {
            return null;
        }

        String packagePath = clazz.getPackage().getName().replace('.', File.separatorChar);
        String fileName = clazz.getSimpleName() + ".java";

        // 尝试多种可能的源码路径转换
        String[] possibleSourcePaths = {
                convertClassPathToSourcePath(classLocation, "target/classes", "src/main/java"),
                convertClassPathToSourcePath(classLocation, "target/test-classes", "src/test/java"),
                convertClassPathToSourcePath(classLocation, "build/classes/java/main", "src/main/java"),
                convertClassPathToSourcePath(classLocation, "build/classes/java/test", "src/test/java"),
                convertClassPathToSourcePath(classLocation, "out/production/classes", "src/main/java"),
                convertClassPathToSourcePath(classLocation, "out/test/classes", "src/test/java")
        };

        for (String sourcePath : possibleSourcePaths) {
            if (sourcePath != null) {
                File sourceFile = new File(sourcePath, packagePath + File.separator + fileName);
                if (sourceFile.exists()) {
                    return sourceFile;
                }
            }
        }

        return null;
    }

    /**
     * 转换类路径到源码路径
     */
    private String convertClassPathToSourcePath(String classLocation, String classDir, String sourceDir) {
        if (classLocation.contains(classDir)) {
            return classLocation.replace(classDir, sourceDir);
        }
        return null;
    }

    /**
     * 在项目中查找源文件
     */
    private File findSourceInProject(String moduleName, Class<?> clazz) throws IOException {
        String packageName = clazz.getPackage().getName();
        String className = clazz.getSimpleName();
        String packagePath = packageName.replace('.', File.separatorChar);
        String fileName = className + ".java";

        File projectRoot = new File(System.getProperty("user.dir")).getAbsoluteFile();
        List<File> searchRoots = new ArrayList<>();

        // 优先使用指定模块（包含其子模块）的源根
        if (moduleName != null && !moduleName.isEmpty()) {
            File moduleDir = new File(projectRoot, moduleName);
            if (moduleDir.exists()) {
                searchRoots.addAll(getModuleSourceRoots(moduleDir));
            }
        }

        // 回退到项目全局源根（递归扫描包含 pom.xml 的模块及其 src/main/java 与 src/test/java）
        for (File root : getProjectSourceRoots(projectRoot)) {
            if (!searchRoots.contains(root)) {
                searchRoots.add(root);
            }
        }

        for (File srcRoot : searchRoots) {
            File sourceFile = new File(srcRoot, packagePath + File.separator + fileName);
            if (sourceFile.exists()) {
                return sourceFile;
            }
        }

        throw new IOException("Source file not found for class: " + packageName + "." + className);
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
     * Extract field information.
     */
    private void extractFields(ClassOrInterfaceDeclaration cls, ClassMetadata classMetadata) {
        for (FieldDeclaration fieldDecl : cls.getFields()) {
            // Skip static fields
            if (fieldDecl.isStatic()) {
                continue;
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
    private String extractComment(Node node) {
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
    private boolean isPrimaryKey(String fieldName) {
        return "id".equals(fieldName) || fieldName.endsWith("Id");
    }

    // Cached project/module source roots utilities
    private synchronized List<File> getProjectSourceRoots(File projectRoot) {
        if (!PROJECT_ROOTS_SCANNED.get()) {
            PROJECT_SOURCE_ROOTS.clear();
            collectModuleSourceRootsRecursive(projectRoot, 0, 5, PROJECT_SOURCE_ROOTS);
            PROJECT_ROOTS_SCANNED.set(true);
        }
        return PROJECT_SOURCE_ROOTS;
    }

    private List<File> getModuleSourceRoots(File moduleDir) {
        List<File> roots = new ArrayList<>();
        collectModuleSourceRootsRecursive(moduleDir, 0, 5, roots);
        return roots;
    }

    private void collectModuleSourceRootsRecursive(File dir, int depth, int maxDepth, List<File> out) {
        if (dir == null || !dir.isDirectory() || depth > maxDepth) return;

        File pom = new File(dir, "pom.xml");
        if (pom.exists()) {
            File main = new File(dir, SRC_MAIN_JAVA);
            if (main.exists() && out.stream().noneMatch(f -> f.getAbsolutePath().equals(main.getAbsolutePath()))) {
                out.add(main);
            }
            File test = new File(dir, SRC_TEST_JAVA);
            if (test.exists() && out.stream().noneMatch(f -> f.getAbsolutePath().equals(test.getAbsolutePath()))) {
                out.add(test);
            }
        }

        File[] children = dir.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                String name = child.getName();
                if (name.equals("target") || name.equals("build") || name.startsWith(".")) {
                    continue;
                }
                collectModuleSourceRootsRecursive(child, depth + 1, maxDepth, out);
            }
        }
    }
}