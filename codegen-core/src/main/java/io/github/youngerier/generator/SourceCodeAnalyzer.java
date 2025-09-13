package io.github.youngerier.generator;

import io.github.youngerier.generator.model.ClassMetadata;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * 源码分析器，使用JavaParser解析Java源码并提取类元数据信息
 */
@Slf4j
public class SourceCodeAnalyzer {

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
            String className = clazz.getName();
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
                .replaceAll("[/\\\\]target[/\\\\]classes", File.separator + "src" + File.separator + "main" + File.separator + "java")
                .replaceAll("[/\\\\]build[/\\\\]classes[/\\\\]java[/\\\\]main", File.separator + "src" + File.separator + "main" + File.separator + "java");
            
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
        
        // Try common source directory structures
        String[] possiblePaths = {
            "src" + File.separator + "main" + File.separator + "java",
            "example" + File.separator + "src" + File.separator + "main" + File.separator + "java",
            "example" + File.separator + "sub-module" + File.separator + "src" + File.separator + "main" + File.separator + "java"
        };
        
        for (String possiblePath : possiblePaths) {
            String filePath = possiblePath + File.separator + packagePath + File.separator + fileName;
            File sourceFile = new File(filePath);
            if (sourceFile.exists()) {
                return sourceFile;
            }
        }
        
        throw new IOException("Source file not found for class: " + packageName + "." + simpleClassName);
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
            Path srcMainJava = current.resolve("src").resolve("main").resolve("java");
            if (srcMainJava.toFile().exists()) {
                return srcMainJava.toFile();
            }
            current = current.getParent();
        }
        
        // Fallback: return the parent directory of the source file's parent directory
        // This assumes the structure is something like src/main/java/package/Class.java
        return sourceFile.getParentFile().getParentFile().getParentFile();
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
                fieldInfo.setType(var.getTypeAsString());
                fieldInfo.setComment(extractComment(fieldDecl));

                // Try to resolve and set the full type
                try {
                    fieldInfo.setFullType(var.getType().resolve().describe());
                } catch (Exception e) {
                    // Fallback to simple type if resolution fails
                    fieldInfo.setFullType(var.getTypeAsString());
                }

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