package com.abc.web.support;

import com.abc.web.support.enums.QueryType;

import java.io.Serializable;
import java.util.List;

public interface IPagination<T> extends Serializable {

    long getTotal();

    List<T> getRecords();

    int getQueryPage();

    int getQuerySize();

    QueryType getQueryType();

    default T getFirst() {
        return getRecords().stream().findFirst().orElse(null);
    }

    default boolean hasRecords() {
        return getRecords() != null && !getRecords().isEmpty();
    }
}
