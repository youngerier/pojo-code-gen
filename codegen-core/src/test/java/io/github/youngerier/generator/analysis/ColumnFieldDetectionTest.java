package io.github.youngerier.generator.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ClassMetadataReader#isMappedColumn} 回归：MyBatis-Flex APT 不为
 * Collection/Map 类型字段生成 TableDef 列，生成器必须识别并跳过这些字段。
 */
class ColumnFieldDetectionTest {

    private final JavaParser javaParser;

    ColumnFieldDetectionTest() {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
        this.javaParser = new JavaParser(configuration);
    }

    @Test
    void jdkCollectionsAndMapsAreNotColumns() {
        assertFalse(ClassMetadataReader.isMappedColumn(resolveField("java.util.List<java.lang.String> v")));
        assertFalse(ClassMetadataReader.isMappedColumn(resolveField("java.util.Set<java.lang.Integer> v")));
        assertFalse(ClassMetadataReader.isMappedColumn(
                resolveField("java.util.Map<java.lang.String, java.lang.Object> v")));
    }

    @Test
    void customContainerSubtypeIsDetectedViaAncestors() {
        String source = """
                import java.util.AbstractList;
                class MyList extends AbstractList<String> {
                    public String get(int index) { return null; }
                    public int size() { return 0; }
                }
                class T { MyList v; }
                """;

        assertFalse(ClassMetadataReader.isMappedColumn(resolve(source)));
    }

    @Test
    void scalarFieldsAreColumns() {
        assertTrue(ClassMetadataReader.isMappedColumn(resolveField("java.lang.String v")));
        assertTrue(ClassMetadataReader.isMappedColumn(resolveField("java.math.BigDecimal v")));
        assertTrue(ClassMetadataReader.isMappedColumn(resolveField("int v")));
    }

    /**
     * 把字段声明包进一个完整类中解析；resolve() 要求节点位于 CompilationUnit 内。
     */
    private ResolvedType resolveField(String fieldDeclaration) {
        return resolve("class T {\n" + fieldDeclaration + ";\n}");
    }

    private ResolvedType resolve(String source) {
        CompilationUnit compilationUnit = javaParser.parse(source).getResult().orElseThrow();
        FieldDeclaration declaration = compilationUnit.getType(compilationUnit.getTypes().size() - 1)
                .asClassOrInterfaceDeclaration().getFields().get(0);
        return declaration.getVariable(0).getType().resolve();
    }
}
