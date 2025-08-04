package com.abc.web.support;

import lombok.Data;

@Data
public class Response<T> {
    private int code;
    private String message;
    private T data;


    public static <T> Response ok(T data) {
        Response<T> res = new Response<>();
        res.setCode(200);
        res.setMessage("success");
        res.setData(data);
        return res;
    }

    public boolean isOk() {
        return code == 200;
    }
}
