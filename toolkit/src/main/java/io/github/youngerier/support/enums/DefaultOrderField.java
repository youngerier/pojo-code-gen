package io.github.youngerier.support.enums;

import io.github.youngerier.support.page.QueryOrderField;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DefaultOrderField implements QueryOrderField {

    /**
     * ID
     */
    ID("id"),

    /**
     * 创建日期
     */
    GMT_CREATE("gmt_create"),

    /**
     * 编辑日期
     */
    GMT_MODIFIED("gmt_modified"),

    /**
     * 排序
     */
    ORDER_INDEX("order_index"),

    /**
     * 状态
     */
    STATE("state"),

    /**
     * 是否启用
     */
    ENABLED("is_enabled");

    /**
     * 排序字段
     */
    private final String orderField;

    private static final DefaultOrderField[] CREATE_ORDER_FIELDS = QueryOrderField.of(DefaultOrderField.GMT_CREATE);

    private static final DefaultOrderField[] MODIFIED_ORDER_FIELDS = QueryOrderField.of(DefaultOrderField.GMT_MODIFIED);

    /**
     * @return 返回按照创建时间排序
     */
    public static DefaultOrderField[] gmtCreate() {
        return CREATE_ORDER_FIELDS;
    }

    /**
     * @return 返回按照更新时间排序
     */
    public static DefaultOrderField[] gmtModified() {
        return MODIFIED_ORDER_FIELDS;
    }
}