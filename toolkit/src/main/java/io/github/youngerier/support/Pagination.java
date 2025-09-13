package io.github.youngerier.support;


import io.github.youngerier.support.enums.QueryType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     *
     * @param page  记录列表
     * @param query 查询参数
     * @param total 总数
     * @return 分页结果
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
     * 转换分页结果类型
     *
     * @param source    源分页结果
     * @param converter 转换函数
     * @return 转换后的分页结果
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
     *
     * @param query 查询参数
     * @return 空分页结果
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

    /**
     * 计算总页数
     *
     * @param total    总记录数
     * @param pageSize 每页大小
     * @return 总页数
     */
    public static int calculateTotalPages(long total, int pageSize) {
        if (pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / pageSize);
    }

    /**
     * 计算偏移量
     *
     * @param page     页码（从1开始）
     * @param pageSize 每页大小
     * @return 偏移量
     */
    public static long calculateOffset(int page, int pageSize) {
        return (long) (Math.max(1, page) - 1) * pageSize;
    }

    /**
     * 验证分页参数
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param maxSize  最大每页大小
     */
    public static void validatePageParams(int page, int pageSize, int maxSize) {
        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("每页大小必须大于0");
        }
        if (pageSize > maxSize) {
            throw new IllegalArgumentException("每页大小不能超过" + maxSize);
        }
    }
}
