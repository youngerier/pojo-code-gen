package io.github.youngerier.generator.analysis;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.nio.file.Path;

/**
 * 一个待生成模型的输入：源文件路径、已解析的编译单元（其解析配置携带符号求解器）
 * 与目标顶层类型声明。
 *
 * @param sourceFile      源文件路径
 * @param compilationUnit 已解析的编译单元
 * @param type            目标顶层类声明
 */
public record ModelInput(Path sourceFile,
                         CompilationUnit compilationUnit,
                         ClassOrInterfaceDeclaration type) {

    public String packageName() {
        return compilationUnit.getPackageDeclaration()
                .map(declaration -> declaration.getNameAsString())
                .orElse("");
    }

    public String qualifiedName() {
        String packageName = packageName();
        return packageName.isEmpty() ? type.getNameAsString() : packageName + "." + type.getNameAsString();
    }
}
