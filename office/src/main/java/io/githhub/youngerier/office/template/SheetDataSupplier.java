package io.githhub.youngerier.office.template;

import io.githhub.youngerier.office.ExportExcelDataFetcher;
import io.githhub.youngerier.office.export.SpringExpressionRowDataFormatter;
import io.githhub.youngerier.office.metadata.ExcelCellDescriptor;
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
        // titles
        List<String> titleRows = titles.stream()
                .map(ExcelCellDescriptor::getTitle)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (!titleRows.isEmpty()) {
            result.add(titleRows);
        }
        fetchers.forEach(fetcher -> {
            int queryPage = 1;
            while (true) {
                List<?> records = fetcher.fetch(queryPage, fetchSize);
                for (Object row : records) {
                    result.add(formatter.formatRows(row));
                }
                if (records.size() < fetchSize) {
                    break;
                }
                queryPage++;
            }
        });
        return result;
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
