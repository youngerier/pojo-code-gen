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
import java.io.FileInputStream;
import java.io.IOException;

/**
 * POJO解析器，使用JavaParser解析POJO类提取信息
 */
@Slf4j
public class PojoParser {

    /**
     * 解析POJO文件
     *
     * @param filePath POJO文件路径
     * @return 解析后的POJO信息
     * @throws IOException IO异常
     */
    public PojoInfo parse(String filePath) throws IOException {
        // 配置符号解析器
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java")));

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        File file = new File(filePath);
        CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(file));

        PojoInfo pojoInfo = new PojoInfo();

        // 提取包名
        cu.getPackageDeclaration().ifPresent(pkg -> pojoInfo.setPackageName(pkg.getNameAsString()));

        // 提取类信息
        cu.getClassByName(file.getName().replace(".java", "")).ifPresent(cls -> {
            pojoInfo.setClassName(cls.getNameAsString());
            pojoInfo.setClassComment(extractComment(cls));

            // 提取字段信息
            extractFields(cls, pojoInfo);
        });

        return pojoInfo;
    }

    /**
     * 提取字段信息
     */
    private void extractFields(ClassOrInterfaceDeclaration cls, PojoInfo pojoInfo) {
        for (FieldDeclaration fieldDecl : cls.getFields()) {
            // 跳过静态字段
            if (fieldDecl.isStatic()) {
                continue;
            }

            for (VariableDeclarator var : fieldDecl.getVariables()) {
                PojoInfo.FieldInfo fieldInfo = new PojoInfo.FieldInfo();
                fieldInfo.setName(var.getNameAsString());
                fieldInfo.setType(var.getTypeAsString());
                fieldInfo.setComment(extractComment(fieldDecl));

                // 尝试解析并设置完整类型
                try {
                    fieldInfo.setFullType(var.getType().resolve().describe());
                } catch (Exception e) {
                    // 解析失败，回退到简单类型
                    fieldInfo.setFullType(var.getTypeAsString());
                }

                // 判断是否为主键（简单判断：字段名是id或xxxId）
                fieldInfo.setPrimaryKey(isPrimaryKey(fieldInfo.getName()));

                pojoInfo.getFields().add(fieldInfo);
            }
        }
    }

    /**
     * 提取注释内容
     *
     * @param node 包含注释的节点
     * @return 提取的注释内容，如果没有注释则返回空字符串
     */
    private String extractComment(Node node) {
        return node.getComment().map(comment -> {
            String commentStr = comment.getContent().trim();
            // 清理注释标记
            return commentStr.replaceAll("\\*", "").trim();
        }).orElse("");
    }

    /**
     * 判断是否为主键字段
     */
    private boolean isPrimaryKey(String fieldName) {
        return "id".equals(fieldName) || fieldName.endsWith("Id");
    }

    // 注释：在JavaParser 3.25.4中，不存在com.github.javaparser.ast.Commentable接口
    // 所有AST节点都继承自Node类，Node类有getComment()方法
}