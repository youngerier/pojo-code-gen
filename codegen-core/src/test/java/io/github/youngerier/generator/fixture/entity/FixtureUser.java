package io.github.youngerier.generator.fixture.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import io.github.youngerier.generator.annotation.GenModel;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 集成测试夹具实体：{@code @GenModel} 标注的 POJO，用于驱动一次完整的代码生成。
 *
 * <p>刻意不继承父类：{@code SourceFileLocator.findSrcMainJavaDir} 只会注册
 * {@code src/main/java} 作为 JavaParser 的源根，测试源码里的父类字段解析不到，
 * 会让断言依赖不确定行为。
 */
@Table(value = FixtureUser.TABLE_NAME)
@Data
@GenModel
public class FixtureUser {

    public static final String TABLE_NAME = "t_fixture_user";

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
     * 用户类型
     */
    private FixtureUserTypeEnum userType;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;
}
