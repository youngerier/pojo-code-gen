# Spring Boot 审计组件配置示例

## application.yml 配置

```yaml
# Spring Boot JSON 配置（用于审计数据序列化）
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    serialization:
      write-dates-as-timestamps: false
      fail-on-empty-beans: false
    deserialization:
      fail-on-unknown-properties: false
      
# 审计组件线程池配置
audit:
  thread-pool:
    core-size: 2
    max-size: 4
    queue-capacity: 1000
    keep-alive-seconds: 60
```

## Spring Boot 应用配置

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.yourpackage", "com.abc.web.support"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 自定义 ObjectMapper（可选）

如果需要特殊的序列化配置，可以在应用中配置：

```java
@Configuration
public class JacksonConfig {
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        
        // 审计相关的序列化配置
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        // 其他自定义配置...
        
        return mapper;
    }
}
```

## 使用说明

1. **默认配置**：审计组件会自动使用Spring Boot提供的默认ObjectMapper
2. **自定义配置**：通过application.yml的spring.jackson配置项进行定制
3. **高级配置**：如需复杂配置，可以定义自己的ObjectMapper Bean

## 优势

- ✅ 使用Spring Boot标准配置方式
- ✅ 减少重复配置，避免Bean冲突
- ✅ 与应用的JSON配置保持一致
- ✅ 支持Spring Boot的配置属性自动绑定