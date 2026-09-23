package io.github.youngerier.support.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum QueryType {

    COUNT_TOTAL("统计总数"),
    QUERY_RECORDS("查询结果集"),
    QUERY_BOTH("查询总数和结果集");

    private final String desc;
}
