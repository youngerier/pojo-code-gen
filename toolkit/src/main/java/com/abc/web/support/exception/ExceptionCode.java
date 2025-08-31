package com.abc.web.support.exception;

public interface ExceptionCode {
    /**
     * 统一表示成功的 code
     */
    String SUCCESSFUL_CODE = "0";

    /**
     * 表示成功的 code
     */
    ExceptionCode SUCCESSFUL = new ExceptionCode() {
        @Override
        public String getCode() {
            return SUCCESSFUL_CODE;
        }

        @Override
        public String getDesc() {
            return "操作成功";
        }
    };

    /**
     * @return 异常码
     */
    String getCode();

    /**
     * @return 异常描述
     */
    String getDesc();
}
