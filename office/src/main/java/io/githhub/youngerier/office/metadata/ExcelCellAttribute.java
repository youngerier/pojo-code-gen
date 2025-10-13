package io.githhub.youngerier.office.metadata;

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
