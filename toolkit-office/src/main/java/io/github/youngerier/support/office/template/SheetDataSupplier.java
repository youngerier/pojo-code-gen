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
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * sheet 数据 提供者
 *
 **/
@Getter
abstract class SheetDataSupplier implements Supplier<List<List<String>>> {

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

    @Override
    public List<List<String>> get() {
        List<List<String>> result = new ArrayList<>();
        fetchers.forEach(fetcher -> {
            int queryPage = 1;
            while (true) {
                List<?> records = fetcher.fetch(queryPage, fetchSize);
                if (records == null) {
                    throw new IllegalStateException(
                            "ExportExcelDataFetcher.fetch must not return null, page = " + queryPage);
                }
                for (Object row : records) {
                    result.add(formatter.formatRows(row));
                }
                if (records.size() < fetchSize) {
                    break;
                }
                if (queryPage >= ExportExcelDataFetcher.MAX_FETCH_PAGES) {
                    throw new IllegalStateException("导出行数超过安全上限 " + ExportExcelDataFetcher.MAX_FETCH_PAGES
                            + " 页，请检查 ExportExcelDataFetcher 是否忽略了 page 参数");
                }
                queryPage++;
            }
        });
        return result;
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
