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
import java.net.URL;
import java.nio.file.Paths;

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
            
            // Configure the symbol solver
            CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
            combinedTypeSolver.add(new ReflectionTypeSolver());
            
            // Determine the source path based on the current working directory
            String userDir = System.getProperty("user.dir");
            String srcPath;
            
            if (userDir.endsWith(moduleName)) {
                // We're already in the module directory
                srcPath = userDir + File.separator + "src" + File.separator + "main" + File.separator + "java";
            } else {
                // We're in the parent directory
                srcPath = userDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java";
            }
            
            combinedTypeSolver.add(new JavaParserTypeSolver(new File(srcPath)));

            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
            StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

            // Find source file using Class object information
            String packagePath = packageName.replace('.', File.separatorChar);
            String fileName = simpleClassName + ".java";
            String filePath = srcPath + File.separator + packagePath + File.separator + fileName;
            
            File sourceFile = new File(filePath);
            if (!sourceFile.exists()) {
                // Try to find the source file using classpath
                URL resource = clazz.getResource(simpleClassName + ".class");
                if (resource != null) {
                    // Try to derive source path from classpath
                    String classPath = Paths.get(resource.toURI()).toString();
                    // Replace target/classes with src/main/java
                    String sourcePath = classPath.replaceAll("target[/\\\\]classes", "src/main/java");
                    sourcePath = sourcePath.replaceAll("build[/\\\\]classes[/\\\\]java[/\\\\]main", "src/main/java");
                    // Replace class file with java file
                    sourcePath = sourcePath.replaceAll(simpleClassName + "\\.class$", simpleClassName + ".java");
                    sourceFile = new File(sourcePath);
                }
                
                if (!sourceFile.exists()) {
                    throw new IOException("Source file not found: " + filePath);
                }
            }

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
        // Configure the symbol solver
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        
        // Determine the source path based on the current working directory
        String userDir = System.getProperty("user.dir");
        String srcPath;
        
        if (userDir.endsWith(moduleName)) {
            // We're already in the module directory
            srcPath = userDir + File.separator + "src" + File.separator + "main" + File.separator + "java";
        } else {
            // We're in the parent directory
            srcPath = userDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java";
        }
        
        combinedTypeSolver.add(new JavaParserTypeSolver(new File(srcPath)));

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        // Extract package and class name from the fully qualified class name
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException("Invalid class name: " + className + ". Expected fully qualified class name.");
        }
        
        String packageName = className.substring(0, lastDotIndex);
        String simpleClassName = className.substring(lastDotIndex + 1);
        String fileName = simpleClassName + ".java";
        String packagePath = packageName.replace('.', File.separatorChar);
        String filePath = srcPath + File.separator + packagePath + File.separator + fileName;
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            throw new IOException("Source file not found: " + filePath);
        }

        CompilationUnit cu = StaticJavaParser.parse(sourceFile);

        ClassMetadata classMetadata = new ClassMetadata();

        // Extract package name
        cu.getPackageDeclaration().ifPresent(pkg -> classMetadata.setPackageName(pkg.getNameAsString()));

        // Extract class information
        cu.getClassByName(simpleClassName).ifPresent(cls -> {
            classMetadata.setClassName(cls.getNameAsString());
            classMetadata.setClassComment(extractComment(cls));

            // Extract field information
            extractFields(cls, classMetadata);
        });

        return classMetadata;
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