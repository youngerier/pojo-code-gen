package io.github.youngerier.support.office.template;

import io.github.youngerier.support.office.metadata.ExcelCellDescriptor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ExcelTemplateRender} 端到端回归：row 模式分批直写、cells 模式列转行，
 * 以及表头在两种模式下都不与数据错位。
 */
class ExcelTemplateRenderTest {

    public static class Person {

        public String name;
        public int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    private static List<ExcelCellDescriptor> descriptors() {
        return List.of(
                ExcelCellDescriptor.of("姓名", "name"),
                ExcelCellDescriptor.of("年龄", "age"));
    }

    private static List<String> readSheetRows(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file);
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            assertEquals(1, workbook.getNumberOfSheets());
            XSSFSheet sheet = workbook.getSheetAt(0);
            List<String> rows = new ArrayList<>();
            for (Row row : sheet) {
                StringBuilder line = new StringBuilder();
                for (Cell cell : row) {
                    line.append(cell.getStringCellValue()).append('|');
                }
                rows.add(line.toString());
            }
            return rows;
        }
    }

    @Test
    void rowModeWritesAllBatchesIntoOneSheet() throws IOException {
        Path file = Files.createTempFile("row-mode", ".xlsx");
        List<Object> data = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            data.add(new Person("p" + i, 20 + i));
        }

        ExcelTemplateRender.withPath(file)
                .sheets(0, "Sheet1")
                .rows()
                .titles(descriptors())
                .data(data)
                .build()
                .render();

        List<String> rows = readSheetRows(file);
        assertEquals(601, rows.size(), "必须包含表头 + 600 行数据");
        assertEquals("姓名|年龄|", rows.get(0));
        assertEquals("p0|20|", rows.get(1));
        assertEquals("p599|619|", rows.get(600));
    }

    @Test
    void cellModeTransposesColumnsIntoRows() throws IOException {
        Path file = Files.createTempFile("cell-mode", ".xlsx");
        List<Object> columns = List.of(
                new Person("A", 1),
                new Person("B", 2));

        ExcelTemplateRender.withPath(file)
                .sheets(0, "Sheet1")
                .cells()
                .titles(descriptors())
                .data(columns)
                .build()
                .render();

        List<String> rows = readSheetRows(file);
        assertEquals(List.of(
                "姓名|年龄|",
                "A|B|",
                "1|2|"), rows);
    }
}
