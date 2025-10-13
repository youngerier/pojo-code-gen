package io.github.youngerier.support.page;

import io.github.youngerier.support.enums.QueryType;

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

    default boolean isEmpty() {
        return !hasRecords();
    }

    default int getTotalPages() {
        if (getQuerySize() <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) getTotal() / getQuerySize());
    }

    default boolean hasNext() {
        return getQueryPage() < getTotalPages();
    }

    default boolean hasPrevious() {
        return getQueryPage() > 1;
    }

    default int getRecordCount() {
        return getRecords() != null ? getRecords().size() : 0;
    }
}
