package io.github.youngerier.support.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 排序方向
 */
@AllArgsConstructor
@Getter
public enum QueryOrderType {

    DESC("降序"),

    ASC("升序");

    private final String desc;
}
