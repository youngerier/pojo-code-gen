package com.abc.entity.web.support;

import com.abc.entity.web.support.enums.QueryOrderType;
import com.abc.entity.web.support.enums.QueryType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface PageQuery<OrderField> {

    /**
     * @return 查询页码
     */
    @NonNull
    Integer getQueryPage();

    void setQueryPage(@NonNull Integer queryPage);

    /**
     * @return 查询大小
     */
    @NonNull
    Integer getQuerySize();

    void setQuerySize(@NonNull Integer querySize);

    /**
     * @return 查询类型
     */
    @NonNull
    QueryType getQueryType();

    void setQueryType(@NonNull QueryType queryType);

    /**
     * 排序字段和排序类型安装数组顺序一一对应
     *
     * @return 排序字段
     */
    OrderField[] getOrderFields();

    void setOrderFields(OrderField[] orderFields);

    /**
     * @return 排序类型
     */
    @Nullable
    QueryOrderType[] getOrderTypes();

    void setOrderTypes(@NonNull QueryOrderType[] orderTypes);
}