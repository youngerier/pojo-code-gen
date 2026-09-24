package io.github.youngerier.support.office;

import io.github.youngerier.support.enums.DescriptiveEnum;
import io.github.youngerier.support.office.metadata.ExcelCellDescriptor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归：{@code createDefaultPrinterByClass} 里 {@code isAssignableFrom} 方向写反、
 * 以及 {@code java.util.Date} 被交给只接受 {@code TemporalAccessor} 的 formatter。
 */
class ExcelCellQuickBuilderTest {

    enum Status implements DescriptiveEnum {
        ACTIVE("启用"),
        DISABLED("禁用");

        private final String desc;

        Status(String desc) {
            this.desc = desc;
        }

        @Override
        public String getDesc() {
            return desc;
        }
    }

    @SuppressWarnings("unused")
    static class SampleDto {
        private Status status;
        private List<String> tags;
        private Date gmtCreate;
        private LocalDateTime gmtModified;
        private LocalDate birthday;
        private boolean enabled;
        private Object extra;
    }

    private static ExcelCellDescriptor descriptorOf(String expression) {
        return ExcelCellQuickBuilder.forClass(SampleDto.class).stream()
                .filter(descriptor -> expression.equals(descriptor.getExpression()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing descriptor for " + expression));
    }

    /**
     * 回归：原实现 {@code clazz.isAssignableFrom(DescriptiveEnum.class)} 对真实枚举恒为 false，
     * 导致枚举列永远打印 name() 而不是 getDesc()。
     */
    @Test
    void enumFieldIsPrintedWithItsDescription() {
        ExcelCellDescriptor descriptor = descriptorOf("status");

        assertEquals("启用", descriptor.getPrinter().print(Status.ACTIVE, Locale.CHINA));
        assertEquals("禁用", descriptor.getPrinter().print(Status.DISABLED, Locale.CHINA));
    }

    /**
     * 回归：原实现 {@code clazz.isAssignableFrom(Collection.class)} 对 {@code List} 为 false，
     * 集合列会退化成 {@code String.valueOf(list)} → "[a, b]"。
     */
    @Test
    void listFieldIsPrintedAsCommaSeparatedValues() {
        ExcelCellDescriptor descriptor = descriptorOf("tags");

        assertEquals("a,b", descriptor.getPrinter().print(List.of("a", "b"), Locale.CHINA));
    }

    /**
     * {@code java.util.Date} 使用专用 formatter，不能按 {@code TemporalAccessor} 处理。
     */
    @Test
    void dateFieldDoesNotThrowClassCastException() {
        ExcelCellDescriptor descriptor = descriptorOf("gmtCreate");
        Date date = new Date(0L);

        // 不断言具体时刻（依赖默认时区），只断言不抛异常且格式正确
        String printed = assertDoesNotThrow(() -> descriptor.getPrinter().print(date, Locale.CHINA));
        assertTrue(printed.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "java.util.Date 应被格式化为 yyyy-MM-dd HH:mm:ss，实际为 " + printed);
    }

    @Test
    void localDateTimeAndLocalDateStillWork() {
        ExcelCellDescriptor dateTime = descriptorOf("gmtModified");
        assertEquals("2024-01-02 03:04:05",
                dateTime.getPrinter().print(LocalDateTime.of(2024, 1, 2, 3, 4, 5), Locale.CHINA));

        ExcelCellDescriptor date = descriptorOf("birthday");
        assertEquals("2024-01-02", date.getPrinter().print(LocalDate.of(2024, 1, 2), Locale.CHINA));
    }

    @Test
    void booleanFieldIsPrintedWithChineseLabels() {
        ExcelCellDescriptor descriptor = descriptorOf("enabled");

        assertEquals("是", descriptor.getPrinter().print(Boolean.TRUE, Locale.CHINA));
        assertEquals("否", descriptor.getPrinter().print(Boolean.FALSE, Locale.CHINA));
    }

    /**
     * 回归：原实现对 {@code Object} 这类超类型会走进 {@code ofEnum}，
     * 因 {@code Object.class.isEnum()} 为 false 而抛 BaseException，整个 DTO 都无法构建。
     */
    @Test
    void superTypeFieldDoesNotBreakDescriptorBuilding() {
        List<ExcelCellDescriptor> descriptors = assertDoesNotThrow(() -> ExcelCellQuickBuilder.forClass(SampleDto.class));

        ExcelCellDescriptor extra = descriptorOf("extra");
        assertEquals(String.valueOf("any"), extra.getPrinter().print("any", Locale.CHINA));
        assertEquals(descriptors.size(), ExcelCellQuickBuilder.forClass(SampleDto.class).size());
    }
}
