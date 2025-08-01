package com.abc.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
public class User {

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
