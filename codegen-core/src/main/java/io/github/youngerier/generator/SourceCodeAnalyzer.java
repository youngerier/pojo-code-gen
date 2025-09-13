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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        // 更智能地确定源代码路径
        String srcPath = findSourcePath(className, moduleName);
        if (srcPath == null) {
            throw new IOException("无法找到模块 " + moduleName + " 的源代码目录");
        }

        // 配置符号解析器
        configureSymbolSolver(srcPath);

        // 构造源文件路径
        String filePath = constructSourceFilePath(className, srcPath);
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            throw new IOException("Source file not found: " + filePath);
        }

        // 解析源文件
        CompilationUnit cu = StaticJavaParser.parse(sourceFile);
        return extractClassMetadata(cu, className);
    }

    /**
     * 配置JavaParser的符号解析器
     * @param srcPath 源代码路径
     */
    private void configureSymbolSolver(String srcPath) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File(srcPath)));

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }

    /**
     * 构造源文件路径
     * @param className 类的全限定名
     * @param srcPath 源代码路径
     * @return 源文件路径
     */
    private String constructSourceFilePath(String className, String srcPath) {
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException("Invalid class name: " + className + ". Expected fully qualified class name.");
        }

        String packageName = className.substring(0, lastDotIndex);
        String simpleClassName = className.substring(lastDotIndex + 1);
        String fileName = simpleClassName + ".java";
        String packagePath = packageName.replace('.', File.separatorChar);
        return srcPath + File.separator + packagePath + File.separator + fileName;
    }

    /**
     * 提取类元数据信息
     * @param cu 编译单元
     * @param className 类的全限定名
     * @return 类元数据
     */
    private ClassMetadata extractClassMetadata(CompilationUnit cu, String className) {
        ClassMetadata classMetadata = new ClassMetadata();

        // 提取包名
        cu.getPackageDeclaration().ifPresent(pkg -> classMetadata.setPackageName(pkg.getNameAsString()));

        // 提取类名
        int lastDotIndex = className.lastIndexOf('.');
        String simpleClassName = lastDotIndex == -1 ? className : className.substring(lastDotIndex + 1);

        // 提取类信息
        cu.getClassByName(simpleClassName).ifPresent(cls -> {
            classMetadata.setClassName(cls.getNameAsString());
            classMetadata.setClassComment(extractComment(cls));
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

        // 首先尝试通过类名反向查找源文件路径
        Optional<String> reversePath = findModulePathByReverseSearch(className, userDir);
        if (reversePath.isPresent()) {
            log.info("通过反向搜索找到源代码目录: {}", reversePath.get());
            return reversePath.get();
        }

        // 定义源代码路径查找策略
        List<SourcePathStrategy> strategies = createSourcePathStrategies(userDir, moduleName);

        // 按策略查找源代码路径
        for (SourcePathStrategy strategy : strategies) {
            String path = strategy.findPath();
            if (path != null && isValidSourcePath(path, className)) {
                log.info("找到源代码目录: {} (策略: {})", path, strategy.getName());
                return path;
            }
        }

        // 如果以上都没找到，尝试在当前目录及其子目录中搜索src/main/java
        String searchedPath = searchSourcePathInSubdirectories(userDir, className);
        if (searchedPath != null) {
            log.info("通过搜索找到源代码目录: {}", searchedPath);
            return searchedPath;
        }

        log.error("无法找到源代码目录，请检查项目结构");
        return null;
    }

    /**
     * 通过类名反向查找模块路径
     * 从类的包名反推可能的模块路径，避免硬编码特定目录结构
     * @param className 类的全限定名
     * @param userDir 用户目录
     * @return 模块路径
     */
    private Optional<String> findModulePathByReverseSearch(String className, String userDir) {
        try {
            // 根据类名构造可能的文件路径
            String packagePath = className.replace('.', File.separatorChar);
            
            // 在当前目录及其子目录中搜索包含该类的src/main/java目录
            Path startPath = Paths.get(userDir);
            return Files.walk(startPath, 10)
                .filter(path -> path.toString().endsWith("src" + File.separator + "main" + File.separator + "java"))
                .filter(Files::isDirectory)
                .filter(path -> {
                    String filePath = path.toString() + File.separator + packagePath + ".java";
                    return new File(filePath).exists();
                })
                .map(path -> {
                    // 找到包含该类的src/main/java目录后，尝试确定模块根目录
                    return findModuleRoot(path, className);
                })
                .filter(path -> path != null && !path.isEmpty())
                .findFirst();
        } catch (IOException e) {
            log.debug("反向搜索模块路径时发生错误: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 从src/main/java路径反推模块根目录
     * @param srcPath src/main/java路径
     * @param className 类名
     * @return 模块根目录
     */
    private String findModuleRoot(Path srcPath, String className) {
        try {
            // 获取src/main/java的父目录（可能是模块根目录）
            Path moduleRoot = srcPath.getParent().getParent();
            
            // 检查是否存在pom.xml或其他模块标识文件
            Path pomFile = moduleRoot.resolve("pom.xml");
            if (Files.exists(pomFile)) {
                // 验证这个模块是否包含我们要找的类
                String packagePath = className.replace('.', File.separatorChar);
                String filePath = srcPath.toString() + File.separator + packagePath + ".java";
                if (new File(filePath).exists()) {
                    return srcPath.toString();
                }
            }
            
            // 如果没有找到pom.xml，返回src/main/java路径
            return srcPath.toString();
        } catch (Exception e) {
            log.debug("确定模块根目录时发生错误: {}", e.getMessage());
            return srcPath.toString(); // fallback到src/main/java路径
        }
    }

    /**
     * 创建源代码路径查找策略
     * @param userDir 用户目录
     * @param moduleName 模块名称
     * @return 策略列表
     */
    private List<SourcePathStrategy> createSourcePathStrategies(String userDir, String moduleName) {
        List<SourcePathStrategy> strategies = new ArrayList<>();
        
        // 当前目录下的标准结构
        strategies.add(new SourcePathStrategyImpl("标准结构", 
            userDir + File.separator + "src" + File.separator + "main" + File.separator + "java"));
        
        // 当前目录就是模块目录的情况
        strategies.add(new SourcePathStrategyImpl("当前目录为模块目录", 
            userDir + File.separator + "src" + File.separator + "main" + File.separator + "java"));
        
        // 父目录结构 (userDir/moduleName/src/main/java)
        strategies.add(new SourcePathStrategyImpl("父目录结构", 
            userDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java"));
        
        // 兄弟目录结构 (userDir/../moduleName/src/main/java)
        strategies.add(new SourcePathStrategyImpl("兄弟目录结构", 
            Paths.get(userDir).getParent().toString() + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java"));
        
        // 更复杂的嵌套结构 (userDir/../../moduleName/src/main/java)
        strategies.add(new SourcePathStrategyImpl("嵌套目录结构", 
            Paths.get(userDir).getParent().getParent().toString() + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java"));
            
        return strategies;
    }

    /**
     * 在子目录中搜索源代码路径
     * @param userDir 用户目录
     * @param className 类名
     * @return 找到的路径，未找到返回null
     */
    private String searchSourcePathInSubdirectories(String userDir, String className) {
        try {
            Path startPath = Paths.get(userDir);
            return Files.walk(startPath, 5)
                .filter(path -> path.toString().endsWith("src" + File.separator + "main" + File.separator + "java"))
                .filter(Files::isDirectory)
                .filter(path -> isValidSourcePath(path.toString(), className))
                .findFirst()
                .map(Path::toString)
                .orElse(null);
        } catch (IOException e) {
            log.warn("搜索源代码目录时发生错误: {}", e.getMessage());
            return null;
        }
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

    /**
     * 源代码路径查找策略接口
     */
    private interface SourcePathStrategy {
        String getName();
        String findPath();
    }

    /**
     * 具体的源代码路径查找策略实现
     */
    private static class SourcePathStrategyImpl implements SourcePathStrategy {
        private final String name;
        private final String path;

        public SourcePathStrategyImpl(String name, String path) {
            this.name = name;
            this.path = path;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String findPath() {
            File srcDir = new File(path);
            return srcDir.exists() && srcDir.isDirectory() ? path : null;
        }
    }
}