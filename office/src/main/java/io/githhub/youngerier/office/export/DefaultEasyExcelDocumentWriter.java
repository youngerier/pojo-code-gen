package io.githhub.youngerier.office.export;

import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import com.alibaba.excel.write.style.row.SimpleRowHeightStyleStrategy;
import io.githhub.youngerier.office.ExcelDocumentWriter;
import io.githhub.youngerier.office.metadata.ExcelCellDescriptor;

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

    private final ExcelWriterSheetBuilder sheetBuilder;

    private final SpringExpressionRowDataFormatter formatter;

    private DefaultEasyExcelDocumentWriter(List<ExcelCellDescriptor> descriptors, ExcelWriterSheetBuilder sheetBuilder) {
        this.rows = new ArrayList<>(2000);
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
        return new DefaultEasyExcelDocumentWriter(descriptors, builder.sheet());
    }

    @Override
    public void write(Collection<Object> rows) {
        this.rows.addAll(rows.stream().map(formatter::formatRows).collect(Collectors.toList()));
    }

    @Override
    public void finish() {
        sheetBuilder.doWrite(rows);
        rows.clear();
    }

}
