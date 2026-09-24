package io.github.youngerier.generator.fixture.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import io.github.youngerier.generator.annotation.GenModel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 集成测试夹具实体：{@code @GenModel} 标注的 POJO，用于驱动一次完整的代码生成。
 *
 * <p>刻意不继承父类：避免父类字段解析依赖不确定行为，断言全部字段都在本类声明。
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
     * 标价（美元，如 $5：注释中的 $ 必须转义，否则 JavaPoet 崩溃）
     */
    private BigDecimal price;

    /**
     * 标签：集合字段不是 MyBatis-Flex 映射列，APT 不会生成 TableRef 列。
     * Repository/Query 必须跳过它，但 DTO/Request 仍应保留。
     */
    private List<String> tags;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;
}
