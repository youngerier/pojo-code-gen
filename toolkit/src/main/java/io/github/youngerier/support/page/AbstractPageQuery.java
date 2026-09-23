package io.github.youngerier.support.page;

import io.github.youngerier.support.enums.QueryOrderType;
import io.github.youngerier.support.enums.QueryType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.lang.NonNull;

/**
 * 分页查询参数基类
 */
@Data
public abstract class AbstractPageQuery<OrderField extends QueryOrderField> {

    /**
     * 单次查询最大条数，避免查询页面数据过大拖垮数据库
     */
    public static final int MAX_QUERY_SIZE = 3000;

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    /**
     * 查询页码（从 1 开始）
     */
    @NotNull
    private Integer queryPage = DEFAULT_PAGE;

    /**
     * 每页条数
     */
    @NotNull
    private Integer querySize = DEFAULT_SIZE;

    /**
     * 查询类型
     */
    private QueryType queryType = QueryType.QUERY_BOTH;

    /**
     * 排序字段，与 {@link #orderTypes} 按数组顺序一一对应
     */
    private OrderField[] orderFields;

    /**
     * 排序类型
     */
    private QueryOrderType[] orderTypes;

    public void setQueryPage(@NonNull Integer queryPage) {
        if (queryPage == null || queryPage < 1) {
            // 在此处拦截：MyBatis-Flex 的 Page 要求 pageNumber >= 1，
            // 否则会在执行查询时抛 IllegalArgumentException 并被兜底成 HTTP 500
            throw new IllegalArgumentException("查询页码必须大于等于 1");
        }
        this.queryPage = queryPage;
    }

    public void setQuerySize(@NonNull Integer querySize) {
        if (querySize == null || querySize < 1) {
            // 同理：Page 要求 pageSize > 0
            throw new IllegalArgumentException("查询大小必须大于等于 1");
        }
        if (querySize > MAX_QUERY_SIZE) {
            throw new IllegalArgumentException("查询大小不能超过" + MAX_QUERY_SIZE);
        }
        this.querySize = querySize;
    }

    @NotNull
    public QueryType getQueryType() {
        return queryType == null ? QueryType.QUERY_BOTH : queryType;
    }

    /**
     * 页码别名，兼容主流框架的 pageNumber 命名
     */
    public Integer getPageNumber() {
        return queryPage;
    }

    public void setPageNumber(Integer pageNumber) {
        // 必须委托给 setQueryPage：直接赋值会绕过页码下界校验
        setQueryPage(pageNumber);
    }

    /**
     * 每页条数别名，兼容主流框架的 pageSize 命名
     */
    public Integer getPageSize() {
        return querySize;
    }

    public void setPageSize(Integer pageSize) {
        setQuerySize(pageSize);
    }

    /**
     * 是否需要处理排序：排序字段与排序类型都存在且长度一致
     */
    public boolean requireOrderBy() {
        if (orderFields == null || orderTypes == null) {
            return false;
        }
        return orderFields.length > 0 && orderFields.length == orderTypes.length;
    }
}
