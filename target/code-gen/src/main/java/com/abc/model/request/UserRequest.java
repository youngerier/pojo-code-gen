package com.abc.model.request;

import lombok.Data;

/**
 * 用户实体
 * 请求参数对象
 */
@Data
public class UserRequest {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
