package io.github.youngerier.generator;

import io.github.youngerier.generator.model.ClassMetadata;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
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

    /**
     * Standard Maven source directory path.
     */
    private static final String SRC_MAIN_JAVA = "src" + File.separator + "main" + File.separator + "java";

    /**
     * Parse a POJO class using Class object.
     *
     * @param clazz       The Class object of the POJO.
     * @param moduleName  The name of the module.
     * @return The parsed POJO information.
     * @throws IOException            If an I/O error occurs.
     */
    public ClassMetadata parse(Class<?> clazz, String moduleName) throws IOException {
        try {
            // Get class information directly from Class object
            String packageName = clazz.getPackage().getName();
            String simpleClassName = clazz.getSimpleName();
            
            // Find the source file path
            File sourceFile = findSourceFile(clazz, packageName, simpleClassName);
            
            // Configure the symbol solver with the parent directory of the source file
            CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
            combinedTypeSolver.add(new ReflectionTypeSolver());
            
            // Get the src/main/java directory
            File srcDir = findSrcMainJavaDir(sourceFile);
            combinedTypeSolver.add(new JavaParserTypeSolver(srcDir));

            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
            StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

            CompilationUnit cu = StaticJavaParser.parse(sourceFile);

            ClassMetadata classMetadata = new ClassMetadata();

            // Extract package name
            classMetadata.setPackageName(packageName);

            // Extract class information
            cu.getClassByName(simpleClassName).ifPresent(cls -> {
                classMetadata.setClassName(cls.getNameAsString());
                classMetadata.setClassComment(extractComment(cls));

                // Extract field information
                extractFields(cls, classMetadata);
            });

            return classMetadata;
        } catch (Exception e) {
            throw new IOException("Failed to parse class: " + clazz.getName(), e);
        }
    }

    /**
     * Parse a POJO class using class name.
     *
     * @param className   The fully qualified class name of the POJO.
     * @param moduleName  The name of the module.
     * @return The parsed POJO information.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class is not found.
     */
    public ClassMetadata parse(String className, String moduleName) throws IOException, ClassNotFoundException {
        // Load the class
        Class<?> clazz = Class.forName(className);
        
        // Delegate to the other parse method
        return parse(clazz, moduleName);
    }

    /**
     * Find the source file for a class.
     *
     * @param clazz The class to find the source file for.
     * @param packageName The package name of the class.
     * @param simpleClassName The simple class name.
     * @return The source file.
     * @throws IOException If the source file cannot be found.
     * @throws URISyntaxException If there is an error with the URI.
     */
    private File findSourceFile(Class<?> clazz, String packageName, String simpleClassName) throws IOException, URISyntaxException {
        // Try to find the source file using classpath
        URL resource = clazz.getResource(simpleClassName + ".class");
        if (resource != null) {
            // Derive source path from classpath
            String classPath = Paths.get(resource.toURI()).toString();
            
            // Replace target/classes or build/classes with src/main/java
            String sourcePath = classPath
                .replaceAll("[/\\\\]target[/\\\\]classes", File.separator + SRC_MAIN_JAVA)
                .replaceAll("[/\\\\]build[/\\\\]classes[/\\\\]java[/\\\\]main", File.separator + SRC_MAIN_JAVA);
            
            // For multi-level modules, we might need to adjust the path further
            // Handle cases like target/classes -> ../../src/main/java
            if (!sourcePath.contains(SRC_MAIN_JAVA)) {
                sourcePath = adjustPathForMultiModule(sourcePath, simpleClassName);
            }
            
            // Replace class file with java file
            sourcePath = sourcePath.replaceAll(simpleClassName + "\\.class$", simpleClassName + ".java");
            
            File sourceFile = new File(sourcePath);
            if (sourceFile.exists()) {
                return sourceFile;
            }
        }
        
        // Fallback: try to construct the path based on package structure
        String packagePath = packageName.replace('.', File.separatorChar);
        String fileName = simpleClassName + ".java";
        
        // Try to find src/main/java by walking up from the current directory
        File currentDir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (currentDir != null) {
            File srcDir = new File(currentDir, SRC_MAIN_JAVA);
            if (srcDir.exists()) {
                File sourceFile = new File(srcDir, packagePath + File.separator + fileName);
                if (sourceFile.exists()) {
                    return sourceFile;
                }
            }
            currentDir = currentDir.getParentFile();
        }
        
        // For multi-level modules, try a more extensive search
        return findSourceFileInMultiModuleStructure(packageName, simpleClassName);
    }

    /**
     * Adjust path for multi-level Maven modules.
     *
     * @param classPath The original class path.
     * @param simpleClassName The simple class name.
     * @return Adjusted path for source file.
     */
    private String adjustPathForMultiModule(String classPath, String simpleClassName) {
        // Handle multi-level module structure
        // For example: /project/module1/module2/target/classes -> /project/module1/module2/src/main/java
        // Or: /project/module1/module2/target/classes -> /project/src/main/java
        
        Path path = Paths.get(classPath);
        Path current = path;
        
        // Walk up the directory tree to find a suitable src/main/java
        int maxDepth = 10;
        for (int i = 0; i < maxDepth && current != null; i++) {
            Path srcMainJava = current.getParent().resolve(SRC_MAIN_JAVA);
            if (srcMainJava.toFile().exists()) {
                return srcMainJava.resolve(Paths.get(classPath).getFileName().toString()).toString();
            }
            current = current.getParent();
        }
        
        // If not found, try a more general approach
        return classPath.replaceFirst("[/\\\\]target[/\\\\]classes.*", 
            File.separator + SRC_MAIN_JAVA + 
            classPath.substring(classPath.indexOf(simpleClassName) - 1));
    }

    /**
     * Find source file in multi-module structure.
     *
     * @param packageName The package name.
     * @param simpleClassName The simple class name.
     * @return The source file.
     * @throws IOException If the source file cannot be found.
     */
    private File findSourceFileInMultiModuleStructure(String packageName, String simpleClassName) throws IOException {
        String packagePath = packageName.replace('.', File.separatorChar);
        String fileName = simpleClassName + ".java";
        
        // Get the current working directory
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            Path userDirPath = Paths.get(userDir).toAbsolutePath().normalize();
            
            // Search in the current directory and its subdirectories
            File sourceFile = searchForSourceFile(userDirPath, packagePath, fileName);
            if (sourceFile != null) {
                return sourceFile;
            }
        }
        
        throw new IOException("Source file not found for class: " + packageName + "." + simpleClassName);
    }

    /**
     * Recursively search for source file in directory structure.
     *
     * @param rootPath The root path to start searching from.
     * @param packagePath The package path.
     * @param fileName The file name.
     * @return The source file if found, null otherwise.
     */
    private File searchForSourceFile(Path rootPath, String packagePath, String fileName) {
        File srcMainJava = rootPath.resolve(SRC_MAIN_JAVA).toFile();
        if (srcMainJava.exists()) {
            File sourceFile = new File(srcMainJava, packagePath + File.separator + fileName);
            if (sourceFile.exists()) {
                return sourceFile;
            }
        }
        
        // Recursively search subdirectories
        File[] subDirs = rootPath.toFile().listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                // Skip target and build directories
                if ("target".equals(subDir.getName()) || "build".equals(subDir.getName())) {
                    continue;
                }
                
                File result = searchForSourceFile(subDir.toPath(), packagePath, fileName);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }

    /**
     * Find the src/main/java directory for a source file.
     *
     * @param sourceFile The source file.
     * @return The src/main/java directory.
     */
    private File findSrcMainJavaDir(File sourceFile) {
        Path sourcePath = sourceFile.toPath().toAbsolutePath().normalize();
        
        // Walk up the directory tree to find src/main/java
        Path current = sourcePath.getParent();
        while (current != null) {
            Path srcMainJava = current.resolve(SRC_MAIN_JAVA);
            if (srcMainJava.toFile().exists()) {
                return srcMainJava.toFile();
            }
            current = current.getParent();
        }
        
        // For multi-level Maven modules, we need to search more extensively
        // Walk up further to find the correct src/main/java directory
        current = sourcePath.getParent();
        int maxDepth = 10; // Limit search depth to prevent infinite loops
        int depth = 0;
        
        while (current != null && depth < maxDepth) {
            // Check if current directory contains pom.xml (indicating a Maven module)
            File pomFile = current.resolve("pom.xml").toFile();
            if (pomFile.exists()) {
                // Look for src/main/java in this directory
                Path srcMainJava = current.resolve(SRC_MAIN_JAVA);
                if (srcMainJava.toFile().exists()) {
                    return srcMainJava.toFile();
                }
            }
            
            // Also check common multi-module patterns
            // Try looking in parent directories for src/main/java
            Path parentSrcMainJava = current.resolve(SRC_MAIN_JAVA);
            if (parentSrcMainJava.toFile().exists()) {
                return parentSrcMainJava.toFile();
            }
            
            current = current.getParent();
            depth++;
        }
        
        // Fallback: return the parent directory of the source file's parent directory
        // This assumes the structure is something like src/main/java/package/Class.java
        File parent = sourceFile.getParentFile();
        if (parent != null) {
            parent = parent.getParentFile();
            if (parent != null) {
                parent = parent.getParentFile();
                if (parent != null && parent.exists()) {
                    return parent;
                }
            }
        }
        
        // Last resort: try to find any src/main/java directory by walking up from the project base
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            Path userDirPath = Paths.get(userDir);
            Path srcMainJava = userDirPath.resolve(SRC_MAIN_JAVA);
            if (srcMainJava.toFile().exists()) {
                return srcMainJava.toFile();
            }
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
                
                // 设置字段类型为TypeName
                try {
                    // 尝试解析类型
                    ResolvedType resolvedType = var.getType().resolve();
                    fieldInfo.setFullType(resolvedType.describe());
                    // 使用解析的类型创建TypeName
                    fieldInfo.setType(ClassName.bestGuess(resolvedType.describe()));
                } catch (Exception e) {
                    // 如果解析失败，使用bestGuess方法
                    fieldInfo.setType(ClassName.bestGuess(var.getTypeAsString()));
                    fieldInfo.setFullType(var.getTypeAsString());
                }
                
                fieldInfo.setComment(extractComment(fieldDecl));

                // Check if it's a primary key (simple check: field name is "id" or ends with "Id")
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
        return node.getComment().map(comment -> {
            String commentStr = comment.getContent().trim();
            // Clean up comment markers
            return commentStr.replaceAll("\\*", "").trim();
        }).orElse("");
    }

    /**
     * Check if it's a primary key field.
     */
    private boolean isPrimaryKey(String fieldName) {
        return "id".equals(fieldName) || fieldName.endsWith("Id");
    }
}