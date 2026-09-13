package io.github.youngerier.support.office.metadata;

import org.springframework.lang.Nullable;

/**
 * excel单元格属性
 *
 **/
public interface ExcelCellAttribute<T> {

    /**
     * @return 属性值
     */
    @Nullable
    T getValue();
}
