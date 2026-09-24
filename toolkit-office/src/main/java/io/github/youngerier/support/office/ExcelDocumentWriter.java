package io.github.youngerier.support.office;

import java.util.Collection;
import java.util.Collections;

/**
 * excel writer
 *
 **/
public interface ExcelDocumentWriter {

    /**
     * 按照行写入数据
     *
     * @param row 行数据
     */
    default void write(Object row) {
        write(Collections.singletonList(row));
    }

    /**
     * 批量写入数据
     *
     * @param rows 行数据列表
     */
    void write(Collection<Object> rows);

    /**
     * 写入完成：必须落盘并释放底层 workbook 与输出流。
     */
    void finish();

    /**
     * 放弃写入：任务失败/中断/取消时调用，丢弃产物并释放底层 workbook 与输出流。
     *
     * @implNote 默认实现为空；实现方应提供幂等的丢弃路径，而非调用 {@link #finish()}。
     */
    default void abort() {
        // no-op by default
    }
}
