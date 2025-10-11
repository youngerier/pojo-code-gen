package io.github.youngerier.generator;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
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

/**
 * 源码分析器，使用JavaParser解析Java源码并提取类元数据信息
 */
@Slf4j
public class SourceCodeAnalyzer {

    private static final String SRC_MAIN_JAVA = "src" + File.separator + "main" + File.separator + "java";

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
            // 解析直接父类（仅一层），如果父类是 Object 则跳过
            cls.getExtendedTypes().stream().findFirst().ifPresent(ext -> {
                try {
                    String parentQualifiedName;
                    try {
                        parentQualifiedName = ext.resolve().asReferenceType().getQualifiedName();
                    } catch (Exception resolveEx) {
                        parentQualifiedName = ext.getNameAsString();
                    }
                    if ("java.lang.Object".equals(parentQualifiedName) || "Object".equals(parentQualifiedName)) {
                        return;
                    }
                    try {
                        Class<?> parentClass = Class.forName(parentQualifiedName);
                        File parentSourceFile = findSourceFile(moduleName, parentClass);
                        CompilationUnit parentCu = StaticJavaParser.parse(parentSourceFile);
                        parentCu.getClassByName(parentClass.getSimpleName()).ifPresent(parentCls -> {
                            extractFields(parentCls, classMetadata);
                        });
                    } catch (Exception ignore) {
                        // 父类解析失败时忽略，不影响当前类解析
                    }
                } catch (Exception e) {
                    // 父类解析失败时忽略，不影响当前类解析
                }
            });
        });

        return classMetadata;
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
        try {
            // 尝试使用Class的ProtectionDomain获取源文件位置
            java.security.ProtectionDomain protectionDomain = clazz.getProtectionDomain();
            if (protectionDomain != null) {
                java.security.CodeSource codeSource = protectionDomain.getCodeSource();
                if (codeSource != null && codeSource.getLocation() != null) {
                    // 获取类所在的JAR或目录
                    String classLocation = codeSource.getLocation().getPath();
                    // 如果是class文件，尝试找到对应的源文件
                    if (classLocation.endsWith(".jar")) {
                        // 如果是JAR包，尝试在项目中查找源文件
                        return findSourceInProject(moduleName, clazz);
                    } else {
                        // 如果是目录，直接构建源文件路径
                        String packagePath = clazz.getPackage().getName().replace('.', File.separatorChar);
                        String sourceFilePath = classLocation.replace("target/classes", "src/main/java") +
                                packagePath + File.separator + clazz.getSimpleName() + ".java";
                        File sourceFile = new File(sourceFilePath);
                        if (sourceFile.exists()) {
                            return sourceFile;
                        }
                    }
                }
            }

            // 如果通过ProtectionDomain无法获取，回退到项目搜索
            return findSourceInProject(moduleName, clazz);
        } catch (Exception e) {
            // 如果出现异常，回退到项目搜索
            return findSourceInProject(moduleName, clazz);
        }
    }

    /**
     * 在项目中查找源文件
     */
    private File findSourceInProject(String moduleName, Class<?> clazz) throws IOException {
        String packageName = clazz.getPackage().getName();
        String className = clazz.getSimpleName();
        String packagePath = packageName.replace('.', File.separatorChar);
        String fileName = className + ".java";

        // 获取项目根目录
        File currentDir = new File(System.getProperty("user.dir")).getAbsoluteFile();

        // 如果指定了模块名，优先在该模块中查找
        if (moduleName != null && !moduleName.isEmpty()) {
            File moduleDir = new File(currentDir, moduleName);
            if (moduleDir.exists()) {
                // 尝试在src/main/java目录查找
                File srcMainJava = new File(moduleDir, SRC_MAIN_JAVA);
                if (srcMainJava.exists()) {
                    File sourceFile = new File(srcMainJava, packagePath + File.separator + fileName);
                    if (sourceFile.exists()) {
                        return sourceFile;
                    }
                }

                // 尝试在src/test/java目录查找
                File srcTestJava = new File(moduleDir, "src/test/java");
                if (srcTestJava.exists()) {
                    File sourceFile = new File(srcTestJava, packagePath + File.separator + fileName);
                    if (sourceFile.exists()) {
                        return sourceFile;
                    }
                }
            }
        }

        // 在所有模块中查找
        File[] potentialModules = currentDir.listFiles(File::isDirectory);
        if (potentialModules != null) {
            for (File module : potentialModules) {
                // 检查是否是Maven模块（包含pom.xml）
                File pomFile = new File(module, "pom.xml");
                if (pomFile.exists()) {
                    // 尝试在src/main/java目录查找
                    File srcMainJava = new File(module, SRC_MAIN_JAVA);
                    if (srcMainJava.exists()) {
                        File sourceFile = new File(srcMainJava, packagePath + File.separator + fileName);
                        if (sourceFile.exists()) {
                            return sourceFile;
                        }
                    }

                    // 尝试在src/test/java目录查找
                    File srcTestJava = new File(module, "src/test/java");
                    if (srcTestJava.exists()) {
                        File sourceFile = new File(srcTestJava, packagePath + File.separator + fileName);
                        if (sourceFile.exists()) {
                            return sourceFile;
                        }
                    }
                }
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
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(findSrcMainJavaDir(sourceFile)));

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
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
        return node.getComment()
                .map(comment -> comment.getContent().trim().replaceAll("\\*", "").trim())
                .orElse("");
    }

    /**
     * Check if it's a primary key field.
     */
    private boolean isPrimaryKey(String fieldName) {
        return "id".equals(fieldName) || fieldName.endsWith("Id");
    }
}