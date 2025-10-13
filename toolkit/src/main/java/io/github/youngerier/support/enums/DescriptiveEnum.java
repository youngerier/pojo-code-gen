package io.github.youngerier.support.enums;

public interface DescriptiveEnum {

    /**
     * 枚举名称
     *
     * @return 名称
     */
    default String name() {
        return "";
    }

    /**
     * 枚举描述中文说明
     *
     * @return 中文说明
     */
    String getDesc();

    /**
     * 获取英文的枚举描述说明
     *
     * @return 英文说明
     */
    default String getEnDesc() {
        return "";
    }
}
