package com.abc.web.support;

import com.abc.web.support.enums.QueryOrderType;
import com.abc.web.support.enums.QueryType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.lang.NonNull;

import java.beans.Transient;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public abstract class AbstractPageQuery<OrderField extends QueryOrderField> implements PageQuery<OrderField> {

    /**
     * 避免查询页面数据过大，拖垮数据库
     */
    private static final AtomicInteger MAX_QUERY_SIZE = new AtomicInteger(3000);
    
    /**
     * 默认查询页码
     */
    private static final int DEFAULT_PAGE = 1;
    
    /**
     * 默认查询大小
     */
    private static final int DEFAULT_SIZE = 20;

    /**
     * 查询页码
     */
    @NotNull
    private Integer queryPage = DEFAULT_PAGE;

    /**
     * 查询大小
     */
    @NotNull
    private Integer querySize = DEFAULT_SIZE;

    /**
     * 查询类型
     */
    private QueryType queryType = QueryType.QUERY_BOTH;

    /**
     * 排序字段
     */
    private OrderField[] orderFields;

    /**
     * 排序类型
     */
    private QueryOrderType[] orderTypes;

    @Override
    public void setQuerySize(@NonNull Integer querySize) {
        if (querySize > MAX_QUERY_SIZE.get()) {
            throw new IllegalArgumentException("查询大小不能超过" + MAX_QUERY_SIZE.get());
        }
        this.querySize = querySize;
    }

    @NotNull
    public QueryType getQueryType() {
        return queryType == null ? QueryType.QUERY_BOTH : queryType;
    }

    /**
     * 是否需要处理排序
     *
     * @return <code>true</code> 需要处理排序
     */
    public boolean requireOrderBy() {
        if (orderFields == null || orderTypes == null) {
            return false;
        }
        return orderFields.length > 0 && orderFields.length == orderTypes.length;
    }

    /**
     * 配置查询大小最大值
     *
     * @param querySize 查询大小
     */
    public static void configureMaxQuerySize(int querySize) {
        if (querySize <= 0) {
            throw new IllegalArgumentException("查询大小必须大于0");
        }
        MAX_QUERY_SIZE.set(querySize);
    }

    /**
     * @return 查询大小最大值
     */
    @Transient
    public int getMaxQuerySize() {
        return MAX_QUERY_SIZE.get();
    }

}
