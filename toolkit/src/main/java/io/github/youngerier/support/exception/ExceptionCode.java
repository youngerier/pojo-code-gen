package io.github.youngerier.support.exception;

/**
 * 异常码
 */
public interface ExceptionCode {

    /**
     * @return 异常码（默认与 HTTP 状态码一致，业务码可自定义）
     */
    String getCode();

    /**
     * @return 异常描述，可直接展示给调用方
     */
    String getDesc();
}
