package com.abc.entity;

import io.github.youngerier.generator.annotation.GenModel;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Table(value = User.TABLE_NAME)
@Data
@GenModel
public class User {
    public static final String TABLE_NAME = "t_user";

    /**
     * 主键
     */
    @Id
    private Long id;

    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;

    /**
     * 用户类型
     */
    private UserTypeEnum userType;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;
}
