package com.abc.entity.web.support;


import com.abc.entity.web.support.enums.QueryType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Pagination<T> {
    private final long total;
    private final List<T> records;
    private final int queryPage;
    private final int querySize;
    private final QueryType queryType;
}
