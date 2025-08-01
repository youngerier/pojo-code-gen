package com.abc.entity;

import lombok.Data;

/**
 * 用户实体
 * 数据传输对象(DTO)
 */
@Data
public class UserDTO {
    /**
     * 主键ID
     */
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
