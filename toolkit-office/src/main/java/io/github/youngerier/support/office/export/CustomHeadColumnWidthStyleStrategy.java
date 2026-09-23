package io.github.youngerier.support.office.export;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.style.column.AbstractHeadColumnWidthStyleStrategy;
import io.github.youngerier.support.office.metadata.ExcelCellDescriptor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 自定义表头宽度
 *
 **/
@AllArgsConstructor
public class CustomHeadColumnWidthStyleStrategy extends AbstractHeadColumnWidthStyleStrategy {

    private final List<ExcelCellDescriptor> descriptors;

    private final int defaultWidth;

    public CustomHeadColumnWidthStyleStrategy(List<ExcelCellDescriptor> descriptors) {
        this(descriptors, 20);
    }

    @Override
    protected Integer columnWidth(Head head, Integer columnIndex) {
        if (columnIndex == null || columnIndex < 0 || columnIndex >= descriptors.size()) {
            // 实际写出列数可能多于 descriptor 数（模板表头、其他 handler 追加列），
            // 越界时回退默认宽度，而不是在写文件中途抛 IndexOutOfBoundsException
            return defaultWidth;
        }
        return descriptors.get(columnIndex).getWidth(defaultWidth);
    }
}
