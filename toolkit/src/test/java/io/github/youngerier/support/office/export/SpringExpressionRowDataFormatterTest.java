package io.github.youngerier.support.office.export;

import io.github.youngerier.support.exception.BaseException;
import io.github.youngerier.support.office.metadata.ExcelCellDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归：取值表达式来自配置/数据库，属于「模板」内容，必须用受限的求值上下文，
 * 禁止 {@code T(...)} 类型引用、构造器与静态方法调用，避免配置写入者获得任意代码执行能力。
 */
class SpringExpressionRowDataFormatterTest {

    public static class Row {

        public String name = "alice";
        public int age = 30;
        public String nickname = null;
    }

    private static SpringExpressionRowDataFormatter formatterOf(String expression) {
        return SpringExpressionRowDataFormatter.of(List.of(ExcelCellDescriptor.expression(expression)));
    }

    @Test
    void evaluatesPlainPropertyExpression() {
        List<String> row = formatterOf("name").formatRows(new Row());

        assertEquals(List.of("alice"), row);
    }

    @Test
    void evaluatesNestedPropertyAndConcatenation() {
        assertEquals(List.of("alice-30"),
                formatterOf("name + '-' + age").formatRows(new Row()));
    }

    @Test
    void evaluatesBooleanConditional() {
        assertEquals(List.of("adult"),
                formatterOf("age >= 18 ? 'adult' : 'minor'").formatRows(new Row()));
    }

    @Test
    void nullCellValueBecomesEmptyString() {
        assertEquals(List.of(""), formatterOf("nickname").formatRows(new Row()));
    }

    @Test
    void evaluatesInstanceMethodCall() {
        assertEquals(List.of("ALICE"), formatterOf("name.toUpperCase()").formatRows(new Row()));
    }

    // ---------------- 安全约束 ----------------

    @Test
    void rejectsTypeReferenceToRuntime() {
        SpringExpressionRowDataFormatter formatter =
                formatterOf("T(java.lang.Runtime).getRuntime().exec('calc')");

        assertThrows(RuntimeException.class, () -> formatter.formatRows(new Row()));
    }

    @Test
    void rejectsStaticMethodInvocation() {
        assertThrows(RuntimeException.class,
                () -> formatterOf("T(java.lang.System).exit(1)").formatRows(new Row()));
    }

    @Test
    void rejectsClassForNameTypeReference() {
        assertThrows(RuntimeException.class,
                () -> formatterOf("T(java.lang.Class).forName('java.lang.Runtime')").formatRows(new Row()));
    }

    @Test
    void rejectsConstructorInvocation() {
        assertThrows(RuntimeException.class,
                () -> formatterOf("new java.lang.ProcessBuilder('calc')").formatRows(new Row()));
    }

    /**
     * 非法表达式必须在构建期就失败，而不是等渲染到某一行时才暴露。
     */
    @Test
    void illegalExpressionFailsFastAtConstruction() {
        BaseException ex = assertThrows(BaseException.class, () -> formatterOf("name +"));
        assertTrue(ex.getMessage().contains("非法的单元格取值表达式"), ex.getMessage());
    }
}
