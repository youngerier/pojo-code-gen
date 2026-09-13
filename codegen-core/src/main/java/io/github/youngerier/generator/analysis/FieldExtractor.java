package io.github.youngerier.generator.analysis;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.resolution.types.ResolvedType;
import com.squareup.javapoet.ClassName;
import io.github.youngerier.generator.model.ClassMetadata;

/**
 * 字段提取器，从类声明中提取实例字段（跳过静态字段）并填充到 {@link ClassMetadata}。
 */
final class FieldExtractor {

    private FieldExtractor() {
    }

    static void extract(ClassOrInterfaceDeclaration cls, ClassMetadata metadata) {
        for (FieldDeclaration fieldDecl : cls.getFields()) {
            if (fieldDecl.isStatic()) {
                continue;
            }
            for (VariableDeclarator variable : fieldDecl.getVariables()) {
                metadata.getFields().add(toFieldInfo(fieldDecl, variable));
            }
        }
    }

    private static ClassMetadata.FieldInfo toFieldInfo(FieldDeclaration fieldDecl, VariableDeclarator variable) {
        ClassMetadata.FieldInfo fieldInfo = new ClassMetadata.FieldInfo();
        fieldInfo.setName(variable.getNameAsString());

        try {
            ResolvedType resolvedType = variable.getType().resolve();
            fieldInfo.setFullType(resolvedType.describe());
            fieldInfo.setType(ClassName.bestGuess(resolvedType.describe()));
        } catch (Exception e) {
            // 类型解析失败时使用原始类型字符串
            String typeString = variable.getTypeAsString();
            fieldInfo.setFullType(typeString);
            fieldInfo.setType(ClassName.bestGuess(typeString));
        }

        fieldInfo.setComment(Comments.extract(fieldDecl));
        fieldInfo.setPrimaryKey(isPrimaryKey(fieldInfo.getName()));
        return fieldInfo;
    }

    private static boolean isPrimaryKey(String fieldName) {
        return "id".equals(fieldName) || fieldName.endsWith("Id");
    }
}
