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
     * 放弃写入：任务失败/中断/取消时调用，用于释放底层 workbook 与输出流。
     *
     * <p>此前失败路径完全不释放 writer，POI workbook 与调用方的 {@code OutputStream}
     * 只能依赖 GC 的 {@code finalize} 兜底，导致文件句柄与内存长期占用。
     *
     * @implNote 默认实现为空，保证既有实现二进制兼容；推荐实现为 {@code finish()} 的幂等替代路径。
     */
    default void abort() {
        // no-op by default
    }
}
