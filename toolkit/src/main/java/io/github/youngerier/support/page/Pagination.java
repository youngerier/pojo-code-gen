package io.github.youngerier.support.page;


import io.github.youngerier.support.enums.QueryType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页结果
 */
@Data
@AllArgsConstructor
public class Pagination<T> implements IPagination<T> {

    private long total;
    private List<T> records;
    private int queryPage;
    private int querySize;
    private QueryType queryType;

    public static <E> Pagination<E> empty() {
        return new Pagination<>(0, List.of(), 0, 0, QueryType.QUERY_BOTH);
    }

    /**
     * 构建分页结果
     */
    public static <T> Pagination<T> of(List<T> page, AbstractPageQuery<?> query, long total) {
        return new Pagination<>(
                total,
                page,
                query.getQueryPage(),
                query.getQuerySize(),
                query.getQueryType()
        );
    }

    /**
     * 转换分页结果中的记录类型
     */
    public static <T, R> Pagination<R> convert(IPagination<T> source, Function<T, R> converter) {
        List<R> convertedRecords = source.getRecords().stream()
                .map(converter)
                .collect(Collectors.toList());

        return new Pagination<>(
                source.getTotal(),
                convertedRecords,
                source.getQueryPage(),
                source.getQuerySize(),
                source.getQueryType()
        );
    }

    /**
     * 创建空分页结果
     */
    public static <T> Pagination<T> empty(AbstractPageQuery<?> query) {
        return new Pagination<>(
                0L,
                Collections.emptyList(),
                query.getQueryPage(),
                query.getQuerySize(),
                query.getQueryType()
        );
    }
}
