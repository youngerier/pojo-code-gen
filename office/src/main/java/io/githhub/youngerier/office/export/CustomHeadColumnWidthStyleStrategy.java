package io.githhub.youngerier.office.export;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.style.column.AbstractHeadColumnWidthStyleStrategy;
import io.githhub.youngerier.office.metadata.ExcelCellDescriptor;
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
        return descriptors.get(columnIndex).getWidth(defaultWidth);
    }
}
