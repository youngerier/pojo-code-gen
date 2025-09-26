package abc.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置类
 */
@Configuration
@MapperScan("**.repository")
public class MybatisConfig {
    
    // 可以在这里添加其他 MyBatis 相关的配置
    // 比如分页插件、类型处理器等
}
