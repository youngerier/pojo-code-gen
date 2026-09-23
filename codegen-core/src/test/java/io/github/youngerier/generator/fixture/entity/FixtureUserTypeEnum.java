package io.github.youngerier.generator.fixture.entity;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 集成测试夹具：枚举字段验证生成的代码能正确引用实体所在包下的自定义类型。
 */
@Getter
@AllArgsConstructor
public enum FixtureUserTypeEnum {

    NORMAL(0, "普通用户"),

    ADMIN(1, "管理员");

    @EnumValue
    private final Integer code;

    private final String desc;
}
