package io.github.youngerier.support.office.metadata;

import io.github.youngerier.support.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.springframework.format.Parser;
import org.springframework.format.Printer;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归：属性收集不允许出现「重复 key 抛不可读异常」，
 * 且 parser/printer 的存在性校验不能互相误判。
 */
class ExcelCellDescriptorTest {

    private static final Parser<String> PARSER = (text, locale) -> text;
    private static final Printer<Object> PRINTER = (value, locale) -> String.valueOf(value);

    @Test
    void duplicateWidthKeepsLastValueInsteadOfThrowing() {
        ExcelCellDescriptor descriptor = assertDoesNotThrow(() ->
                ExcelCellDescriptor.builder("title", "expr").width(10).width(20).build());

        assertEquals(20, descriptor.getWidth(5));
    }

    @Test
    void widthFallsBackToDefaultWhenAbsent() {
        assertEquals(7, ExcelCellDescriptor.of("title", "expr").getWidth(7));
    }

    /**
     * 回归：原实现把 parser 的重复校验写成了 CellPrinter，
     * 于是 parser(...) 与 printer(...) 同时使用时会被误判为「Printer already exists」。
     */
    @Test
    void parserAndPrinterCanCoexist() throws Exception {
        ExcelCellDescriptor descriptor = assertDoesNotThrow(() -> ExcelCellDescriptor.builder("title", "expr")
                .parser(PARSER)
                .printer(PRINTER)
                .build());

        assertTrue(descriptor.hasPrinter());
        assertNotNull(descriptor.getParser().parse("x", Locale.CHINA));
        assertEquals("x", descriptor.getPrinter().print("x", Locale.CHINA));
    }

    @Test
    void secondParserIsRejected() {
        ExcelCellDescriptor.ExcelCellDescriptorBuilder builder =
                ExcelCellDescriptor.builder("title", "expr").parser(PARSER);

        assertThrows(BaseException.class, () -> builder.parser(PARSER));
    }

    @Test
    void secondPrinterIsRejected() {
        ExcelCellDescriptor.ExcelCellDescriptorBuilder builder =
                ExcelCellDescriptor.builder("title", "expr").printer(PRINTER);

        assertThrows(BaseException.class, () -> builder.printer(PRINTER));
    }

    @Test
    void defaultPrinterIsUsedWhenAbsent() throws Exception {
        ExcelCellDescriptor descriptor = ExcelCellDescriptor.of("title", "expr");

        assertEquals("42", descriptor.getPrinter().print(42, Locale.CHINA));
        // 默认 parser 是恒等函数，原样返回文本
        assertEquals("42", descriptor.getParser().parse("42", Locale.CHINA));
    }
}
