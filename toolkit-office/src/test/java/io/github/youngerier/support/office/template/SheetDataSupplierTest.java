package io.github.youngerier.support.office.template;

import io.github.youngerier.support.office.ExportExcelDataFetcher;
import io.github.youngerier.support.office.metadata.ExcelCellDescriptor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归（放在 template 包内以访问包私有类型）：
 *
 * <ul>
 *     <li>表头行不再过滤空标题——过滤会让表头相对数据整体左移一列。</li>
 *     <li>迭代器只产出数据行，表头由 {@link SheetRender} 单独前置，
 *         避免 cells 模式下表头被当成第一列数据参与转置。</li>
 *     <li>取数实现返回 null 或分页永不缩小时要有明确异常/安全上限。</li>
 * </ul>
 */
class SheetDataSupplierTest {

    private static List<ExcelCellDescriptor> titlesWithBlankInMiddle() {
        return List.of(
                ExcelCellDescriptor.builder("名称", "name").build(),
                // 无标题的列：原实现会把它过滤掉，导致后续列全部错位
                ExcelCellDescriptor.expression("city"),
                ExcelCellDescriptor.builder("年龄", "age").build());
    }

    private static <T> SheetDataSupplier rowSupplier(List<ExcelCellDescriptor> titles,
                                                     ExportExcelDataFetcher<T> fetcher, int fetchSize) {
        List<ExportExcelDataFetcher<?>> fetchers = new ArrayList<>();
        fetchers.add(fetcher);
        return new SheetDataSupplier.RowSupplier(titles, fetchers, fetchSize);
    }

    @Test
    void titleRowKeepsColumnAlignmentWhenTitleIsBlank() {
        SheetDataSupplier supplier = rowSupplier(titlesWithBlankInMiddle(), (page, size) -> List.of(), 10);

        assertTrue(supplier.hasTitleRow());
        assertEquals(List.of("名称", "", "年龄"), supplier.titleRow());
        assertEquals(3, supplier.titleRow().size(), "表头列数必须与描述符列数一致");
    }

    @Test
    void hasTitleRowIsFalseWhenAllTitlesBlank() {
        List<ExcelCellDescriptor> noTitles = List.of(
                ExcelCellDescriptor.expression("name"),
                ExcelCellDescriptor.expression("age"));
        SheetDataSupplier supplier = rowSupplier(noTitles, (page, size) -> List.of(), 10);

        assertFalse(supplier.hasTitleRow());
    }

    @Test
    void bodyContainsOnlyDataRows() {
        SheetDataSupplier supplier = rowSupplier(titlesWithBlankInMiddle(),
                (page, size) -> page == 1 ? List.of(new Row("alice", 30, "sh")) : List.of(), 10);

        List<List<String>> body = supplier.collectAll();

        assertEquals(1, body.size(), "表头不应出现在 body 中");
        assertEquals(List.of("alice", "sh", "30"), body.get(0));
    }

    @Test
    void nullFetchResultIsReportedClearly() {
        SheetDataSupplier supplier = rowSupplier(titlesWithBlankInMiddle(), (page, size) -> null, 10);

        IllegalStateException ex = assertThrows(IllegalStateException.class, supplier::collectAll);
        assertTrue(ex.getMessage().contains("must not return null"), ex.getMessage());
    }

    @Test
    void neverShrinkingFetcherIsInterruptedBySafetyLimit() {
        int[] calls = new int[1];
        SheetDataSupplier supplier = rowSupplier(List.of(ExcelCellDescriptor.expression("name")),
                (page, size) -> {
                    calls[0]++;
                    return List.of(new Row("always-full", 1, "x"));
                }, 1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, supplier::collectAll);

        assertTrue(ex.getMessage().contains("安全上限"), ex.getMessage());
        assertEquals(ExportExcelDataFetcher.MAX_FETCH_PAGES, calls[0]);
    }

    /**
     * 惰性：创建迭代器不触发取数，消费到某页时才抓该页，不预取后续页。
     */
    @Test
    void iteratorFetchesPagesLazily() {
        int[] calls = new int[1];
        SheetDataSupplier supplier = rowSupplier(List.of(ExcelCellDescriptor.expression("name")),
                (page, size) -> {
                    calls[0]++;
                    return page <= 3 ? List.of(new Row("page" + page, 1, "x")) : List.of();
                }, 10);

        java.util.Iterator<List<String>> iterator = supplier.iterator();
        assertEquals(0, calls[0], "创建迭代器不应触发取数");

        assertTrue(iterator.hasNext());
        assertEquals(1, calls[0]);
        assertEquals(List.of("page1"), iterator.next());
        assertEquals(1, calls[0], "消费第一行不应预取后续页");
    }

    /** 供 SpEL 取值的行对象 */
    public static class Row {

        public String name;
        public int age;
        public String city;

        public Row(String name, int age, String city) {
            this.name = name;
            this.age = age;
            this.city = city;
        }
    }
}
