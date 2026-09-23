package io.github.youngerier.support.office.export;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import com.alibaba.excel.write.style.row.SimpleRowHeightStyleStrategy;
import io.github.youngerier.support.office.ExcelDocumentWriter;
import io.github.youngerier.support.office.metadata.ExcelCellDescriptor;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 easyexcel 的 excel writer
 *
 * @github https://github.com/alibaba/easyexcel
 **/
public class DefaultEasyExcelDocumentWriter implements ExcelDocumentWriter {

    private final List<Object> rows;

    private final ExcelWriter excelWriter;

    private final ExcelWriterSheetBuilder sheetBuilder;

    private final SpringExpressionRowDataFormatter formatter;

    private DefaultEasyExcelDocumentWriter(List<ExcelCellDescriptor> descriptors, ExcelWriter excelWriter,
                                           ExcelWriterSheetBuilder sheetBuilder) {
        this.rows = new ArrayList<>(2000);
        this.excelWriter = excelWriter;
        this.sheetBuilder = sheetBuilder;
        this.formatter = new SpringExpressionRowDataFormatter(descriptors);
    }

    public static DefaultEasyExcelDocumentWriter of(OutputStream output, List<ExcelCellDescriptor> descriptors) {
        List<WriteHandler> handlers = Arrays.asList(
                new CustomHeadColumnWidthStyleStrategy(descriptors),
                new SimpleRowHeightStyleStrategy((short) 25, (short) 25));
        return of(output, descriptors, handlers);
    }

    public static DefaultEasyExcelDocumentWriter of(OutputStream output, List<ExcelCellDescriptor> descriptors, Collection<WriteHandler> handlers) {
        List<String> titles = descriptors.stream().map(ExcelCellDescriptor::getTitle).collect(Collectors.toList());
        ExcelWriterBuilder builder = new ExcelWriterBuilder();
        builder.file(output)
                .head(titles.stream().map(Collections::singletonList).collect(Collectors.toList()))
                .needHead(true)
                .charset(StandardCharsets.UTF_8);
        for (WriteHandler handler : handlers) {
            builder.registerWriteHandler(handler);
        }
        // 必须自己持有 ExcelWriter：只有它能在失败路径上释放 POI workbook 与输出流
        ExcelWriter excelWriter = builder.build();
        return new DefaultEasyExcelDocumentWriter(descriptors, excelWriter, new ExcelWriterSheetBuilder(excelWriter));
    }

    @Override
    public void write(Collection<Object> rows) {
        this.rows.addAll(rows.stream().map(formatter::formatRows).collect(Collectors.toList()));
    }

    /**
     * 落盘并释放资源。
     *
     * <p>原实现只调用 {@code sheetBuilder.doWrite(...)} 而没有调用 {@link ExcelWriter#finish()}，
     * 数据不会真正刷出（工作簿的最终写入发生在 finish），文件内容不完整且资源只能等 GC 回收。
     */
    @Override
    public void finish() {
        try {
            sheetBuilder.doWrite(rows);
        } finally {
            rows.clear();
            excelWriter.finish();
        }
    }

    /**
     * 放弃写入并释放资源：任务失败/中断/取消路径调用。
     */
    @Override
    public void abort() {
        rows.clear();
        // close() 内部走 finish()，对已 finish 的 context 幂等；主要目的是释放 workbook 与输出流
        excelWriter.close();
    }
}
