package com.example.generator;

import com.example.generator.model.PojoInfo;
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

/**
 * POJO解析器，使用JavaParser解析POJO类提取信息
 */
@Slf4j
public class PojoParser {

    /**
     * Parse a POJO class.
     *
     * @param className  The fully qualified class name of the POJO.
     * @param moduleName The name of the module.
     * @return The parsed POJO information.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class is not found.
     */
    public PojoInfo parse(String className, String moduleName) throws IOException, ClassNotFoundException {
        // Configure the symbol solver
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        String srcPath = System.getProperty("user.dir") + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java";
        combinedTypeSolver.add(new JavaParserTypeSolver(new File(srcPath)));

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        Class<?> clazz = Class.forName(className);
        String fileName = clazz.getSimpleName() + ".java";
        String packagePath = clazz.getPackage().getName().replace('.', File.separatorChar);
        String filePath = srcPath + File.separator + packagePath + File.separator + fileName;

        CompilationUnit cu = StaticJavaParser.parse(new File(filePath));

        PojoInfo pojoInfo = new PojoInfo();

        // Extract package name
        cu.getPackageDeclaration().ifPresent(pkg -> pojoInfo.setPackageName(pkg.getNameAsString()));

        // Extract class information
        cu.getClassByName(clazz.getSimpleName()).ifPresent(cls -> {
            pojoInfo.setClassName(cls.getNameAsString());
            pojoInfo.setClassComment(extractComment(cls));

            // Extract field information
            extractFields(cls, pojoInfo);
        });

        return pojoInfo;
    }

    /**
     * Extract field information.
     */
    private void extractFields(ClassOrInterfaceDeclaration cls, PojoInfo pojoInfo) {
        for (FieldDeclaration fieldDecl : cls.getFields()) {
            // Skip static fields
            if (fieldDecl.isStatic()) {
                continue;
            }

            for (VariableDeclarator var : fieldDecl.getVariables()) {
                PojoInfo.FieldInfo fieldInfo = new PojoInfo.FieldInfo();
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

                pojoInfo.getFields().add(fieldInfo);
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
