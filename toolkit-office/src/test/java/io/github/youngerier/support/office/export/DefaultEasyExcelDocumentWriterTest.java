package io.github.youngerier.support.office.export;

import io.github.youngerier.support.office.metadata.ExcelCellDescriptor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DefaultEasyExcelDocumentWriter} 流式行为回归：
 * 分批直写、Sheet 上限自动拆分、空导出只出表头、finish/abort 幂等且释放资源。
 */
class DefaultEasyExcelDocumentWriterTest {

    public static class Person {

        public String name;
        public int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /**
     * 记录 close() 的输出流：EasyExcel 默认 autoCloseStream=true，finish/abort 都应关闭它。
     */
    private static class TrackingOutputStream extends ByteArrayOutputStream {

        private boolean closed;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private static List<ExcelCellDescriptor> descriptors() {
        return List.of(
                ExcelCellDescriptor.of("姓名", "name"),
                ExcelCellDescriptor.of("年龄", "age"));
    }

    private static DefaultEasyExcelDocumentWriter writer(TrackingOutputStream out, int maxRowsPerSheet) {
        return DefaultEasyExcelDocumentWriter.of(out, descriptors(), List.of(), maxRowsPerSheet);
    }

    private static List<Object> people(int from, int count) {
        List<Object> people = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            people.add(new Person("p" + (from + i), 20 + from + i));
        }
        return people;
    }

    // ---------------- 读取产物 ----------------

    /**
     * 把 xlsx 读回成「每个 Sheet 一组行，每行以 | 拼接单元格」。
     */
    private static List<List<String>> readWorkbook(ByteArrayOutputStream out) throws IOException {
        try (XSSFWorkbook workbook =
                     new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            List<List<String>> sheets = new ArrayList<>();
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                XSSFSheet sheet = workbook.getSheetAt(index);
                List<String> rows = new ArrayList<>();
                for (Row row : sheet) {
                    StringBuilder line = new StringBuilder();
                    for (Cell cell : row) {
                        line.append(cell.getStringCellValue()).append('|');
                    }
                    rows.add(line.toString());
                }
                sheets.add(rows);
            }
            return sheets;
        }
    }

    // ---------------- 空导出 ----------------

    @Test
    void emptyExportWritesHeadOnlySheet() throws IOException {
        TrackingOutputStream out = new TrackingOutputStream();

        writer(out, 1_048_576).finish();

        List<List<String>> sheets = readWorkbook(out);
        assertEquals(1, sheets.size(), "空导出也应产出一个 Sheet");
        assertEquals(List.of("姓名|年龄|"), sheets.get(0), "Sheet 必须只含表头行");
        assertTrue(out.closed, "finish 必须关闭输出流");
    }

    // ---------------- 分批直写 ----------------

    @Test
    void batchesAreFlushedInOrder() throws IOException {
        TrackingOutputStream out = new TrackingOutputStream();
        DefaultEasyExcelDocumentWriter writer = writer(out, 1_048_576);

        writer.write(people(0, 2));
        writer.write(people(2, 1));
        writer.finish();

        List<List<String>> sheets = readWorkbook(out);
        assertEquals(1, sheets.size());
        assertEquals(List.of(
                "姓名|年龄|",
                "p0|20|",
                "p1|21|",
                "p2|22|"), sheets.get(0));
    }

    // ---------------- 自动拆分 ----------------

    /**
     * 上限 5（表头 + 4 行数据），一批写 10 行 → 3 个 Sheet：4/4/2，每个 Sheet 带表头。
     */
    @Test
    void oversizedBatchSplitsAcrossSheets() throws IOException {
        TrackingOutputStream out = new TrackingOutputStream();
        DefaultEasyExcelDocumentWriter writer = writer(out, 5);

        writer.write(people(0, 10));
        writer.finish();

        List<List<String>> sheets = readWorkbook(out);
        assertEquals(3, sheets.size());
        assertEquals(5, sheets.get(0).size());
        assertEquals(5, sheets.get(1).size());
        assertEquals(3, sheets.get(2).size());
        for (List<String> sheet : sheets) {
            assertEquals("姓名|年龄|", sheet.get(0), "每个 Sheet 都必须输出表头");
        }
        assertEquals("p9|29|", sheets.get(2).get(2));
    }

    /**
     * 跨批次也必须在写满时拆分：上限 3（表头 + 2 行），两批各 2 行 → 2 个 Sheet 各 2 行数据。
     */
    @Test
    void splitsWhenFilledAcrossBatches() throws IOException {
        TrackingOutputStream out = new TrackingOutputStream();
        DefaultEasyExcelDocumentWriter writer = writer(out, 3);

        writer.write(people(0, 2));
        writer.write(people(2, 2));
        writer.finish();

        List<List<String>> sheets = readWorkbook(out);
        assertEquals(2, sheets.size());
        assertEquals(List.of("姓名|年龄|", "p0|20|", "p1|21|"), sheets.get(0));
        assertEquals(List.of("姓名|年龄|", "p2|22|", "p3|23|"), sheets.get(1));
    }

    // ---------------- 生命周期 ----------------

    @Test
    void writeAfterFinishThrows() {
        TrackingOutputStream out = new TrackingOutputStream();
        DefaultEasyExcelDocumentWriter writer = writer(out, 1_048_576);

        writer.finish();

        assertThrows(IllegalStateException.class, () -> writer.write(people(0, 1)));
    }

    @Test
    void finishIsIdempotent() {
        TrackingOutputStream out = new TrackingOutputStream();
        DefaultEasyExcelDocumentWriter writer = writer(out, 1_048_576);

        writer.finish();
        writer.finish();
    }

    @Test
    void abortIsIdempotentAndBlocksFurtherWrites() {
        TrackingOutputStream out = new TrackingOutputStream();
        DefaultEasyExcelDocumentWriter writer = writer(out, 1_048_576);
        writer.write(people(0, 3));

        writer.abort();
        writer.abort();

        assertThrows(IllegalStateException.class, () -> writer.write(people(0, 1)));
        assertTrue(out.closed, "abort 必须关闭输出流");
    }

    /**
     * abort 是「丢弃」而非「落盘」：SXSS 在 finish 前不向输出流写字节，
     * 失败任务不得产出半成品文件。
     */
    @Test
    void abortDiscardsWorkbookWithoutProducingFile() throws IOException {
        TrackingOutputStream out = new TrackingOutputStream();
        DefaultEasyExcelDocumentWriter writer = writer(out, 1_048_576);
        writer.write(people(0, 5));

        writer.abort();

        assertEquals(0, out.size(), "abort 后输出流必须仍为空");
    }
}
