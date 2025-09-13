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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 源码分析器，使用JavaParser解析Java源码并提取类元数据信息
 */
@Slf4j
public class SourceCodeAnalyzer {

    /**
     * Parse a POJO class.
     *
     * @param className  The fully qualified class name of the POJO.
     * @param moduleName The name of the module.
     * @return The parsed POJO information.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class is not found.
     */
    public ClassMetadata parse(String className, String moduleName) throws IOException, ClassNotFoundException {
        // Configure the symbol solver
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        
        // 更智能地确定源代码路径
        String srcPath = findSourcePath(className, moduleName);
        if (srcPath == null) {
            throw new IOException("无法找到模块 " + moduleName + " 的源代码目录");
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
     * 智能查找源代码路径
     * @param className 类的全限定名
     * @param moduleName 模块名称
     * @return 源代码路径，如果找不到则返回null
     */
    private String findSourcePath(String className, String moduleName) {
        String userDir = System.getProperty("user.dir");
        log.info("当前工作目录: {}", userDir);
        log.info("模块名称: {}", moduleName);
        
        // 尝试多种可能的源代码路径
        String[] possiblePaths = {
            // 当前目录下的标准结构
            userDir + File.separator + "src" + File.separator + "main" + File.separator + "java",
            
            // 当前目录就是模块目录的情况
            userDir + File.separator + "src" + File.separator + "main" + File.separator + "java",
            
            // 父目录结构 (userDir/moduleName/src/main/java)
            userDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java",
            
            // 兄弟目录结构 (userDir/../moduleName/src/main/java)
            Paths.get(userDir).getParent().toString() + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java",
            
            // 更复杂的嵌套结构 (userDir/../../moduleName/src/main/java)
            Paths.get(userDir).getParent().getParent().toString() + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java",
            
            // ping-biz目录结构 (userDir/ping-biz/moduleName/src/main/java)
            userDir + File.separator + "ping-biz" + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java"
        };
        
        // 检查每个可能的路径
        for (String path : possiblePaths) {
            File srcDir = new File(path);
            if (srcDir.exists() && srcDir.isDirectory() && isValidSourcePath(path, className)) {
                log.info("找到源代码目录: {}", path);
                return path;
            }
        }
        
        // 如果以上都没找到，尝试在当前目录及其子目录中搜索src/main/java
        try {
            Path startPath = Paths.get(userDir);
            Path foundPath = Files.walk(startPath, 5)
                .filter(path -> path.toString().endsWith("src" + File.separator + "main" + File.separator + "java"))
                .filter(Files::isDirectory)
                .filter(path -> isValidSourcePath(path.toString(), className))
                .findFirst()
                .orElse(null);
                
            if (foundPath != null) {
                String path = foundPath.toString();
                log.info("通过搜索找到源代码目录: {}", path);
                return path;
            }
        } catch (IOException e) {
            log.warn("搜索源代码目录时发生错误: {}", e.getMessage());
        }
        
        log.error("无法在以下路径中找到源代码目录:");
        for (String path : possiblePaths) {
            log.error("  - {}", path);
        }
        
        return null;
    }

    /**
     * 验证源代码路径是否有效（包含指定的类文件）
     * @param srcPath 源代码路径
     * @param className 类的全限定名
     * @return 如果路径有效返回true，否则返回false
     */
    private boolean isValidSourcePath(String srcPath, String className) {
        try {
            // 根据类名构造文件路径
            String packagePath = className.replace('.', File.separatorChar);
            String filePath = srcPath + File.separator + packagePath + ".java";
            File sourceFile = new File(filePath);
            
            boolean exists = sourceFile.exists();
            if (!exists) {
                log.debug("路径 {} 中未找到类文件: {}", srcPath, filePath);
            }
            return exists;
        } catch (Exception e) {
            log.debug("验证源代码路径时发生错误: {}", e.getMessage());
            return false;
        }
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