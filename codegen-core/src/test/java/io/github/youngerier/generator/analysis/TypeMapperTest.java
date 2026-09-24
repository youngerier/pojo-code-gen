package io.github.youngerier.generator.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link TypeMapper} 回归测试：JRE 类型即可覆盖全部类型形态，无需项目依赖。
 */
class TypeMapperTest {

    private final JavaParser javaParser;

    TypeMapperTest() {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
        this.javaParser = new JavaParser(configuration);
    }

    @Test
    void mapsParameterizedTypeWithArguments() {
        ResolvedType resolved = resolve("java.util.List<java.lang.String> tags");

        assertEquals(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class)),
                TypeMapper.map(resolved));
    }

    @Test
    void mapsNestedParameterizedType() {
        ResolvedType resolved = resolve("java.util.Map<java.lang.String, "
                + "java.util.List<java.lang.Integer>> nested");

        TypeName expected = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(Integer.class)));
        assertEquals(expected, TypeMapper.map(resolved));
    }

    @Test
    void mapsRawReferenceType() {
        ResolvedType resolved = resolve("java.util.ArrayList raw");

        assertEquals(ClassName.get(java.util.ArrayList.class), TypeMapper.map(resolved));
    }

    @Test
    void mapsPrimitiveAndPrimitiveArray() {
        assertEquals(TypeName.INT, TypeMapper.map(resolve("int count")));
        assertEquals(ArrayTypeName.of(TypeName.INT),
                TypeMapper.map(resolve("int[] values")));
        assertEquals(ArrayTypeName.of(ClassName.get(String.class)),
                TypeMapper.map(resolve("java.lang.String[] names")));
    }

    @Test
    void mapsExtendsAndSuperWildcards() {
        ResolvedType extendsWildcard = resolve(
                "java.util.List<? extends java.lang.Number> numbers");

        assertEquals(
                ParameterizedTypeName.get(ClassName.get(List.class),
                        WildcardTypeName.subtypeOf(ClassName.get(Number.class))),
                TypeMapper.map(extendsWildcard));

        ResolvedType superWildcard = resolve(
                "java.util.List<? super java.lang.String> strings");

        assertEquals(
                ParameterizedTypeName.get(ClassName.get(List.class),
                        WildcardTypeName.supertypeOf(ClassName.get(String.class))),
                TypeMapper.map(superWildcard));
    }

    /**
     * 把字段声明包进一个完整类中解析；resolve() 要求节点位于 CompilationUnit 内。
     */
    private ResolvedType resolve(String fieldDeclaration) {
        CompilationUnit compilationUnit = javaParser.parse("class T {\n" + fieldDeclaration + ";\n}")
                .getResult()
                .orElseThrow(() -> new IllegalStateException("无法解析声明: " + fieldDeclaration));
        FieldDeclaration declaration = compilationUnit.getType(0)
                .asClassOrInterfaceDeclaration().getFields().get(0);
        return declaration.getVariable(0).getType().resolve();
    }
}
