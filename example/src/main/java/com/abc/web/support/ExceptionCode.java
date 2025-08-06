package com.abc.web.support;

public interface ExceptionCode {
    /**
     * 统一表示成功的 code
     */
    String SUCCESSFUL_CODE = "0";

    /**
     * 表示成功的 code
     */
    ExceptionCode SUCCESSFUL = new ExceptionCode() {
        private static final long serialVersionUID = 5034455936657195532L;

        @Override
        public String getCode() {
            return SUCCESSFUL_CODE;
        }

        public String getDesc() {
            return "";
        }
    };


    /**
     * @return 异常码
     */
    String getCode();
}
