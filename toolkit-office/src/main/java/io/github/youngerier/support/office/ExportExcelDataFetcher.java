package io.github.youngerier.support.office;

import java.util.List;

/**
 * excel 导出数据 fetcher
 *
 **/
public interface ExportExcelDataFetcher<T> {

    int DEFAULT_TOTAL = -1;

    /**
     * 翻页安全上限（页数）。
     *
     * <p>导出循环以「本页数据量小于 {@code size} 即结束」作为终止条件。若取数实现忽略了
     * {@code page} 参数、每次都返回满页数据，循环将永不终止；达到本上限时抛出异常中断，
     * 避免线程被无限占用与内存被无限增长。
     */
    int MAX_FETCH_PAGES = 10_000;

    /**
     * 统计总数
     *
     * @return -1 表示不关心总数，查询到没有为止
     */
    default int count() {
        return DEFAULT_TOTAL;
    }

    /**
     * 抓取数据
     *
     * @param page 开始抓取数据的页码 从 1 开始
     * @param size 抓取大小
     * @return 数据集合，查询到结果集小于 size 为止；<strong>不得返回 null</strong>
     */
    List<T> fetch(int page, int size);
}
