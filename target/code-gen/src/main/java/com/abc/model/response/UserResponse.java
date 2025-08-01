package com.abc.model.response;

import lombok.Data;

/**
 * 用户实体
 * 响应对象
 */
@Data
public class UserResponse {
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;
}
