package com.abc.web.support;

public interface QueryOrderField {
    /**
     * @return 需要排序的字段名
     */
    String getOrderField();

    /**
     * 工厂方法，方便用户传参
     *
     * @param fields 排序字段列表
     * @return 排序字段列表
     */
    @SafeVarargs
    static <T extends QueryOrderField> T[] of(T... fields) {
        return fields;
    }
}
