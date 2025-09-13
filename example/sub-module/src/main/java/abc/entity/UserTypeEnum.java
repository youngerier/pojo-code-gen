package com.abc.entity;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserTypeEnum {
    NORMAL(0, "普通用户"),
    ADMIN(1, "管理员"),
    SUPER_ADMIN(2, "超级管理员");

    @EnumValue
    private final Integer code;
    private final String desc;


}
