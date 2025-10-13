package io.github.youngerier.support.exception;

import io.github.youngerier.support.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 异常输出的错误日志等级
 *
 * @author wuxp
 * @date 2024-12-23 10:23
 **/
@AllArgsConstructor
@Getter
public enum ExceptionLogLevel implements DescriptiveEnum {

    /**
     * 不输出日志
     */
    NONE("NONE"),

    ERROR("ERROR"),

    WARN("WARN"),

    INFO("INFO");

    private final String desc;
}
