package com.abc.entity.web.support;


import com.abc.entity.web.support.enums.QueryType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

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

    public static <T> Pagination<T> of(List<T> page, AbstractPageQuery<?> query, long total) {
        return new Pagination<>(
                total,
                page,
                query.getQueryPage(),
                query.getQuerySize(),
                query.getQueryType()
        );
    }
}
