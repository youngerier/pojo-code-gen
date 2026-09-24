package io.github.youngerier.support.office.template;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import io.github.youngerier.support.AssertUtils;
import io.github.youngerier.support.constants.Constants;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * sheet 渲染器
 *
 **/
record SheetRender(int index, String sheetName, List<SheetDataSupplier> suppliers) {

    /**
     * row 模式下每批刷入工作簿的行数
     */
    private static final int WRITE_BATCH_SIZE = 500;

    static SheetRenderBuilder builder(int index, String sheetName) {
        return new SheetRenderBuilder(index, sheetName);
    }

    void render(ExcelWriter excelWriter) {
        boolean cellMode = CollectionUtils.firstElement(suppliers) instanceof SheetDataSupplier.CellSupplier;

        WriteSheet sheet = EasyExcelFactory.writerSheet(sheetName).sheetNo(index).build();

        // 表头单独前置写入：既不参与 row 模式累积，也不参与 cells 模式「列转置」
        List<List<String>> head = new ArrayList<>();
        for (SheetDataSupplier supplier : suppliers) {
            if (supplier.hasTitleRow()) {
                head.add(supplier.titleRow());
            }
        }
        if (!head.isEmpty()) {
            excelWriter.write(head, sheet);
        }

        if (cellMode) {
            renderCellMode(excelWriter, sheet);
        } else {
            renderRowMode(excelWriter, sheet);
        }
    }

    /**
     * row 模式：逐 supplier 迭代，按 {@link #WRITE_BATCH_SIZE} 分批直写，不在内存中累积数据。
     */
    private void renderRowMode(ExcelWriter excelWriter, WriteSheet sheet) {
        for (SheetDataSupplier supplier : suppliers) {
            List<List<String>> batch = new ArrayList<>(WRITE_BATCH_SIZE);
            for (List<String> row : supplier) {
                batch.add(row);
                if (batch.size() >= WRITE_BATCH_SIZE) {
                    excelWriter.write(batch, sheet);
                    batch = new ArrayList<>(WRITE_BATCH_SIZE);
                }
            }
            if (!batch.isEmpty()) {
                excelWriter.write(batch, sheet);
            }
        }
    }

    /**
     * cells 模式：每个 supplier 提供的是「列」数据，必须收齐全部列才能按行转置。
     *
     * <p>该模式无法流式：输出第 0 行就需要每一列的第 0 个单元格，而列按顺序到达，
     * 最后一列的首个单元格只能在全部取数完成后才可知。大数据量导出请走 row 模式或
     * {@code SpringExpressionExportExcelTask} 任务路径。
     */
    private void renderCellMode(ExcelWriter excelWriter, WriteSheet sheet) {
        List<List<String>> columns = suppliers.stream()
                .map(SheetDataSupplier::collectAll)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        List<List<String>> rows = convertColumnsToRows(columns);
        if (!rows.isEmpty()) {
            excelWriter.write(rows, sheet);
        }
    }

    /**
     * 列数据转行数据
     *
     * @param columns 按列的数据
     * @return 行数据
     */
    List<List<String>> convertColumnsToRows(List<List<String>> columns) {
        int maxColumns = columns.stream().mapToInt(List::size).max().orElse(0);
        List<List<String>> result = new ArrayList<>();
        for (int col = 0; col < maxColumns; col++) {
            List<String> column = new ArrayList<>();
            for (List<String> values : columns) {
                column.add(col < values.size() ? values.get(col) : Constants.EMPTY);
            }
            result.add(column);
        }

        return result;
    }


    static class SheetRenderBuilder {

        private final int index;

        private final String sheetName;

        private final List<SheetDataSupplier.SheetDataSupplierBuilder> builders = new ArrayList<>();

        SheetRenderBuilder(int index, String sheetName) {
            this.index = index;
            this.sheetName = sheetName;
        }

        void addDataSupplier(SheetDataSupplier.SheetDataSupplierBuilder builder) {
            this.builders.add(builder);
        }

        @NotNull
        SheetDataSupplier.SheetDataSupplierBuilder latestRender() {
            SheetDataSupplier.SheetDataSupplierBuilder result = CollectionUtils.lastElement(builders);
            AssertUtils.notNull(result, "lasest SheetDataSupplierBuilder must not null");
            return result;
        }

        SheetRender build(int fetchSize) {
            List<SheetDataSupplier> dataSuppliers = builders.stream()
                    .map(b -> b.build(fetchSize))
                    .collect(Collectors.toList());
            Set<Class<?>> classes = dataSuppliers.stream()
                    .map(SheetDataSupplier::getClass)
                    .collect(Collectors.toSet());
            AssertUtils.isTrue(classes.size() == 1, () -> "SheetDataSupplier uses either row mode or column mode. Mixing the two is not allowed.");
            return new SheetRender(index, sheetName, dataSuppliers);
        }
    }

}
