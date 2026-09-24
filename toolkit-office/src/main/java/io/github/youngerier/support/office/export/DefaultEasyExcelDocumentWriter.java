package io.github.youngerier.support.office.export;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.row.SimpleRowHeightStyleStrategy;
import io.github.youngerier.support.office.ExcelDocumentWriter;
import io.github.youngerier.support.office.metadata.ExcelCellDescriptor;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 EasyExcel 的流式 Excel Writer。
 *
 * <p>{@code write} 的每一批数据立即通过 {@link ExcelWriter#write} 刷入 SXSS 流式工作簿，
 * 不再在内存中累积全部行；单个 Sheet 写满 Excel 行数上限时自动拆分到下一个 Sheet
 * （表头随每个 Sheet 自动输出）。底层 workbook 与输出流由 {@link #finish()} /
 * {@link #abort()} 释放。
 *
 * @github https://github.com/alibaba/easyexcel
 **/
public class DefaultEasyExcelDocumentWriter implements ExcelDocumentWriter {

    /**
     * Excel 单个 Sheet 的行数上限（含表头行）。
     */
    static final int MAX_ROWS_PER_SHEET = 1_048_576;

    private final ExcelWriter excelWriter;

    private final SpringExpressionRowDataFormatter formatter;

    /**
     * 单个 Sheet 的行数上限（含表头行）
     */
    private final int maxRowsPerSheet;

    private int nextSheetIndex;

    /**
     * 当前 Sheet 已写入的行数（含表头）；-1 表示尚未创建 Sheet
     */
    private int rowsInCurrentSheet = -1;

    private WriteSheet currentSheet;

    private boolean finished;

    private DefaultEasyExcelDocumentWriter(List<ExcelCellDescriptor> descriptors, ExcelWriter excelWriter) {
        this(descriptors, excelWriter, MAX_ROWS_PER_SHEET);
    }

    /**
     * 包可见构造：允许单测注入更小的 Sheet 行数上限以验证自动拆分。
     */
    DefaultEasyExcelDocumentWriter(List<ExcelCellDescriptor> descriptors,
                                   ExcelWriter excelWriter,
                                   int maxRowsPerSheet) {
        this.excelWriter = excelWriter;
        this.formatter = new SpringExpressionRowDataFormatter(descriptors);
        this.maxRowsPerSheet = maxRowsPerSheet;
    }

    public static DefaultEasyExcelDocumentWriter of(OutputStream output, List<ExcelCellDescriptor> descriptors) {
        List<WriteHandler> handlers = Arrays.asList(
                new CustomHeadColumnWidthStyleStrategy(descriptors),
                new SimpleRowHeightStyleStrategy((short) 25, (short) 25));
        return of(output, descriptors, handlers);
    }

    public static DefaultEasyExcelDocumentWriter of(OutputStream output,
                                                    List<ExcelCellDescriptor> descriptors,
                                                    Collection<WriteHandler> handlers) {
        return of(output, descriptors, handlers, MAX_ROWS_PER_SHEET);
    }

    /**
     * 包可见工厂：允许单测注入更小的 Sheet 行数上限。
     */
    static DefaultEasyExcelDocumentWriter of(OutputStream output,
                                             List<ExcelCellDescriptor> descriptors,
                                             Collection<WriteHandler> handlers,
                                             int maxRowsPerSheet) {
        List<String> titles = descriptors.stream()
                .map(ExcelCellDescriptor::getTitle).collect(Collectors.toList());
        ExcelWriterBuilder builder = new ExcelWriterBuilder();
        builder.file(output)
                .head(titles.stream().map(Collections::singletonList).collect(Collectors.toList()))
                .needHead(true)
                .charset(StandardCharsets.UTF_8);
        for (WriteHandler handler : handlers) {
            builder.registerWriteHandler(handler);
        }
        // 必须自己持有 ExcelWriter：只有它能在失败路径上释放 POI workbook 与输出流
        return new DefaultEasyExcelDocumentWriter(descriptors, builder.build(), maxRowsPerSheet);
    }

    /**
     * 批量写入：一批数据立即落进流式工作簿，跨 Sheet 上限时自动拆分。
     */
    @Override
    public void write(Collection<Object> rows) {
        if (finished) {
            throw new IllegalStateException("writer already finished; cannot write more rows");
        }
        List<List<String>> formatted = rows.stream()
                .map(formatter::formatRows).collect(Collectors.toList());

        int offset = 0;
        while (offset < formatted.size()) {
            if (currentSheet == null) {
                currentSheet = nextSheet();
            }
            int capacity = maxRowsPerSheet - rowsInCurrentSheet;
            if (capacity == 0) {
                // 当前 Sheet 已写满，自动拆分，表头随新 Sheet 自动输出
                currentSheet = nextSheet();
                capacity = maxRowsPerSheet - rowsInCurrentSheet;
            }
            int end = Math.min(formatted.size(), offset + capacity);
            excelWriter.write(formatted.subList(offset, end), currentSheet);
            rowsInCurrentSheet += end - offset;
            offset = end;
        }
    }

    /**
     * 落盘并释放资源。一次导出没有任何数据时，仍写出只含表头的 Sheet。
     */
    @Override
    public void finish() {
        if (finished) {
            return;
        }
        finished = true;
        try {
            if (currentSheet == null) {
                currentSheet = nextSheet();
                excelWriter.write(List.of(), currentSheet);
            }
            excelWriter.finish();
        } finally {
            reset();
        }
    }

    /**
     * 放弃写入并释放资源：任务失败/中断/取消路径调用。
     *
     * <p>不能使用 {@code excelWriter.close()}：它内部等价于 {@code finish()}，
     * 会把半成品工作簿完整刷入输出流，让失败任务产出一个「看起来成功」的文件。
     * 此处走 EasyExcel 的异常结束路径 {@code writeContext.finish(true)}：默认配置
     * （{@code writeExcelOnException=false}）下跳过 {@code workbook.write}，
     * 但仍关闭 workbook、清理 SXSS 临时文件并按 {@code autoCloseStream} 关闭输出流。
     */
    @Override
    public void abort() {
        if (finished) {
            return;
        }
        finished = true;
        try {
            excelWriter.writeContext().finish(true);
        } finally {
            reset();
        }
    }

    private WriteSheet nextSheet() {
        WriteSheet sheet = EasyExcel.writerSheet(nextSheetIndex++).build();
        // 表头在首次 write 到该 Sheet 时由 EasyExcel 自动输出
        rowsInCurrentSheet = 1;
        return sheet;
    }

    private void reset() {
        currentSheet = null;
        rowsInCurrentSheet = -1;
    }
}
