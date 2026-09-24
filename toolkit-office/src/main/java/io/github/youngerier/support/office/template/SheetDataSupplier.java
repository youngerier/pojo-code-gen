package io.github.youngerier.support.office.template;

import io.github.youngerier.support.constants.Constants;
import io.github.youngerier.support.office.ExportExcelDataFetcher;
import io.github.youngerier.support.office.export.SpringExpressionRowDataFormatter;
import io.github.youngerier.support.office.metadata.ExcelCellDescriptor;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * sheet 数据 提供者
 *
 **/
@Getter
abstract class SheetDataSupplier implements Iterable<List<String>> {

    private final List<ExcelCellDescriptor> titles;

    private final List<ExportExcelDataFetcher<?>> fetchers;

    private final int fetchSize;

    private final SpringExpressionRowDataFormatter formatter;

    protected SheetDataSupplier(@NotNull List<ExcelCellDescriptor> titles, @NotNull List<ExportExcelDataFetcher<?>> fetchers, int fetchSize) {
        this.titles = titles;
        this.fetchers = fetchers;
        this.fetchSize = fetchSize;
        this.formatter = SpringExpressionRowDataFormatter.of(titles);
    }

    /**
     * 惰性迭代：每次只抓取并格式化当前页，不在内存中累积全部数据行。
     * 每次调用返回全新的迭代器，可重复迭代。
     */
    @Override
    public Iterator<List<String>> iterator() {
        return new PagedRowIterator();
    }

    /**
     * 把全部数据行收集到 List；仅在确实需要全量数据时使用（例如 cells 模式转置）。
     */
    List<List<String>> collectAll() {
        return StreamSupport.stream(spliterator(), false).collect(Collectors.toList());
    }

    /**
     * 跨多个 fetcher 的分页行迭代器。
     */
    private final class PagedRowIterator implements Iterator<List<String>> {

        private final Iterator<ExportExcelDataFetcher<?>> fetcherIterator = fetchers.iterator();

        private ExportExcelDataFetcher<?> currentFetcher;

        private int queryPage;

        private List<?> pageRecords = List.of();

        private int pageIndex;

        @Override
        public boolean hasNext() {
            while (pageIndex >= pageRecords.size()) {
                if (!loadNextPage()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public List<String> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return formatter.formatRows(pageRecords.get(pageIndex++));
        }

        /**
         * 当前页耗尽时加载下一页，并在 fetcher 之间切换。
         *
         * @return 是否成功加载到数据；false 表示所有 fetcher 都已结束
         */
        private boolean loadNextPage() {
            while (true) {
                if (currentFetcher == null) {
                    if (!fetcherIterator.hasNext()) {
                        return false;
                    }
                    currentFetcher = fetcherIterator.next();
                    queryPage = 1;
                } else {
                    // 上一页是当前 fetcher 的最后一页（页大小不足）→ 切换下一个 fetcher
                    if (pageRecords.size() < fetchSize) {
                        currentFetcher = null;
                        continue;
                    }
                    if (queryPage >= ExportExcelDataFetcher.MAX_FETCH_PAGES) {
                        throw new IllegalStateException("导出行数超过安全上限 " + ExportExcelDataFetcher.MAX_FETCH_PAGES
                                + " 页，请检查 ExportExcelDataFetcher 是否忽略了 page 参数");
                    }
                    queryPage++;
                }

                List<?> records = currentFetcher.fetch(queryPage, fetchSize);
                if (records == null) {
                    throw new IllegalStateException(
                            "ExportExcelDataFetcher.fetch must not return null, page = " + queryPage);
                }
                pageRecords = records;
                pageIndex = 0;
                if (records.isEmpty()) {
                    // 空页等价于页大小不足 → 当前 fetcher 结束
                    currentFetcher = null;
                    continue;
                }
                return true;
            }
        }
    }

    /**
     * 表头行：长度与列数严格一致，标题为空的列写空串。
     *
     * <p>此前表头会把空标题过滤掉，导致表头相对数据整体左移一列。
     */
    List<String> titleRow() {
        return titles.stream()
                .map(descriptor -> StringUtils.hasText(descriptor.getTitle())
                        ? descriptor.getTitle()
                        : Constants.EMPTY)
                .collect(Collectors.toList());
    }

    /**
     * @return 是否存在可展示的表头
     */
    boolean hasTitleRow() {
        return titles.stream().anyMatch(descriptor -> StringUtils.hasText(descriptor.getTitle()));
    }

    static SheetDataSupplierBuilder row() {
        return new SheetDataSupplierBuilder(true);
    }

    static SheetDataSupplierBuilder cel() {
        return new SheetDataSupplierBuilder(false);
    }


    static class SheetDataSupplierBuilder {

        private final boolean rowMode;

        private final List<ExcelCellDescriptor> titles = new ArrayList<>();

        private final List<ExportExcelDataFetcher<?>> fetchers = new ArrayList<>();

        SheetDataSupplierBuilder(boolean rowMode) {
            this.rowMode = rowMode;
        }

        SheetDataSupplierBuilder titles(List<ExcelCellDescriptor> titles) {
            this.titles.addAll(titles);
            return this;
        }


        SheetDataSupplierBuilder data(List<?> data) {
            return this.data(new SplitExcelDataFetcherWrapper(data));
        }

        <T> SheetDataSupplierBuilder data(ExportExcelDataFetcher<T> fetcher) {
            this.fetchers.removeIf(e -> !(e instanceof SplitExcelDataFetcherWrapper));
            this.fetchers.add(fetcher);
            return this;
        }

        SheetDataSupplier build(int fetchSize) {
            return rowMode ? new RowSupplier(titles, fetchers, fetchSize) : new CellSupplier(titles, fetchers, fetchSize);
        }
    }

    static class RowSupplier extends SheetDataSupplier {

        public RowSupplier(@NotNull List<ExcelCellDescriptor> titles, @NotNull List<ExportExcelDataFetcher<?>> fetchers, int fetchSize) {
            super(titles, fetchers, fetchSize);
        }
    }

    static class CellSupplier extends SheetDataSupplier {

        public CellSupplier(@NotNull List<ExcelCellDescriptor> titles, @NotNull List<ExportExcelDataFetcher<?>> fetchers, int fetchSize) {
            super(titles, fetchers, fetchSize);
        }
    }

    @AllArgsConstructor
    private static class SplitExcelDataFetcherWrapper implements ExportExcelDataFetcher<Object> {

        private final List<?> data;

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public List fetch(int page, int size) {
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(page * size, data.size());
            return data.subList(fromIndex, toIndex);
        }
    }

}
