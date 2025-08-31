# Spring Boot 中 ObjectMapper 定制配置指南

## 1. 通过 application.yml 配置（推荐）

这是最简单和推荐的方式，Spring Boot 会自动应用这些配置到默认的 ObjectMapper：

```yaml
spring:
  jackson:
    # 日期时间配置
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    locale: zh_CN
    
    # 序列化配置
    serialization:
      write-dates-as-timestamps: false      # 日期不使用时间戳格式
      write-date-timestamps-as-nanoseconds: false
      fail-on-empty-beans: false            # 允许序列化空对象
      indent-output: false                  # 生产环境不格式化输出
      write-null-map-values: false          # 不序列化null的Map值
      write-empty-json-arrays: true         # 序列化空数组
      
    # 反序列化配置
    deserialization:
      fail-on-unknown-properties: false     # 忽略未知属性
      fail-on-null-for-primitives: false    # 允许基本类型为null
      accept-single-value-as-array: true    # 单值可作为数组
      accept-empty-string-as-null-object: true
      
    # 解析器配置
    parser:
      allow-comments: true                   # 允许JSON注释
      allow-yaml-comments: true
      allow-single-quotes: true              # 允许单引号
      allow-unquoted-field-names: true       # 允许不加引号的字段名
      
    # 生成器配置
    generator:
      write-numbers-as-strings: false       # 数字不作为字符串
      quote-field-names: true               # 字段名加引号
      
    # 映射器配置
    mapper:
      accept-case-insensitive-properties: true  # 忽略属性大小写
      accept-case-insensitive-enums: true       # 忽略枚举大小写
      
    # 属性命名策略
    property-naming-strategy: SNAKE_CASE       # 蛇形命名：user_name
    # 其他选项：
    # LOWER_CAMEL_CASE  驼峰命名：userName (默认)
    # UPPER_CAMEL_CASE  帕斯卡命名：UserName
    # LOWER_CASE        小写：username
    # KEBAB_CASE        短横线：user-name
    
    # 默认包含策略
    default-property-inclusion: NON_NULL       # 只包含非null值
    # 其他选项：
    # ALWAYS            总是包含
    # NON_EMPTY         非空值（不包含空字符串、空集合等）
    # NON_DEFAULT       非默认值
```

## 2. 自定义 ObjectMapper Bean（高级配置）

当需要更复杂的配置时，可以创建自定义的 ObjectMapper Bean：

```java
@Configuration
public class JacksonConfig {
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册所有模块（包括JavaTimeModule等）
        mapper.findAndRegisterModules();
        
        // 日期时间配置
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        mapper.setLocale(Locale.CHINA);
        
        // 序列化配置
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        
        // 反序列化配置
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        
        // 解析器配置
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        
        // 生成器配置
        mapper.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false);
        mapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
        
        // 映射器配置
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        
        // 属性命名策略
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        
        // 包含策略
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // 自定义模块
        mapper.registerModule(createCustomModule());
        
        return mapper;
    }
    
    /**
     * 创建自定义Jackson模块
     */
    private Module createCustomModule() {
        SimpleModule module = new SimpleModule("CustomModule");
        
        // 自定义序列化器
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        
        // 自定义枚举处理
        module.addSerializer(Enum.class, new EnumSerializer());
        module.addDeserializer(Enum.class, new EnumDeserializer());
        
        return module;
    }
}
```

## 6. 环境特定配置

### 6.1 开发环境配置

```yaml
# application-dev.yml
spring:
  jackson:
    serialization:
      indent-output: true              # 开发环境格式化输出，便于调试
    parser:
      allow-comments: true             # 开发环境允许JSON注释
      allow-single-quotes: true
```

### 6.2 生产环境配置

```yaml
# application-prod.yml
spring:
  jackson:
    serialization:
      indent-output: false             # 生产环境压缩输出
    parser:
      allow-comments: false            # 生产环境严格JSON格式
      allow-single-quotes: false
    default-property-inclusion: NON_NULL # 减少响应大小
```

## 7. 自定义序列化器和反序列化器

### 7.1 敏感数据序列化器（用于审计）

```java
public class SensitiveDataSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (value != null && value.length() > 4) {
            String masked = value.substring(0, 2) + "****" + value.substring(value.length() - 2);
            gen.writeString(masked);
        } else {
            gen.writeString("****");
        }
    }
}
```

### 7.2 日期时间序列化器

```java
public class CustomLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        gen.writeString(value.format(FORMATTER));
    }
}
```

### 7.3 枚举序列化器

```java
public class EnumSerializer extends JsonSerializer<Enum<?>> {
    @Override
    public void serialize(Enum<?> value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        // 序列化为对象，包含name和描述
        gen.writeStartObject();
        gen.writeStringField("code", value.name());
        gen.writeStringField("desc", getEnumDescription(value));
        gen.writeEndObject();
    }
    
    private String getEnumDescription(Enum<?> enumValue) {
        // 获取枚举描述的逻辑
        return enumValue.toString();
    }
}
```

## 8. 使用@JsonComponent注解

```java
@JsonComponent
public class CustomJsonComponents {
    
    public static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) 
                throws IOException {
            gen.writeString(value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }
    
    public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) 
                throws IOException {
            return LocalDateTime.parse(p.getValueAsString(), 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}
```

## 9. 审计组件专用配置示例

考虑到审计组件的特殊需求，以下是推荐的配置：

```yaml
# application.yml - 审计优化配置
spring:
  jackson:
    # 基础配置
    date-format: yyyy-MM-dd HH:mm:ss.SSS  # 审计需要精确时间
    time-zone: Asia/Shanghai
    locale: zh_CN
    
    # 序列化配置
    serialization:
      write-dates-as-timestamps: false
      fail-on-empty-beans: false          # 允许序列化空对象
      write-null-map-values: false        # 不序列化null的Map值
      indent-output: false                # 生产环境紧凑输出
      
    # 反序列化配置
    deserialization:
      fail-on-unknown-properties: false   # 审计数据容错性
      accept-empty-string-as-null-object: true
      
    # 包含策略（审计建议包含所有非null值）
    default-property-inclusion: NON_NULL
```

## 10. 最佳实践

### 10.1 配置优先级

1. **application.yml配置** - 适用于大多数场景
2. **@JsonComponent** - 适用于特定类型的序列化
3. **自定义ObjectMapper Bean** - 适用于复杂的全局配置
4. **专用ObjectMapper** - 适用于特定模块（如审计）

### 10.2 性能优化建议

```java
@Configuration
public class PerformanceOptimizedJacksonConfig {
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 性能优化配置
        mapper.getFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        mapper.getFactory().disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        
        // 缓存优化
        mapper.enable(MapperFeature.USE_ANNOTATIONS);
        mapper.disable(MapperFeature.AUTO_DETECT_CREATORS);
        mapper.disable(MapperFeature.AUTO_DETECT_FIELDS);
        mapper.disable(MapperFeature.AUTO_DETECT_GETTERS);
        mapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS);
        mapper.disable(MapperFeature.AUTO_DETECT_SETTERS);
        
        return mapper;
    }
}
```

### 10.3 调试配置

```yaml
# application-debug.yml
spring:
  jackson:
    serialization:
      indent-output: true
    parser:
      allow-comments: true
      allow-yaml-comments: true
    
logging:
  level:
    com.fasterxml.jackson: DEBUG        # 启用Jackson调试日志
```

## 11. 常见问题和解决方案

### 11.1 日期格式问题

```java
// 问题：不同的日期格式
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
private LocalDateTime createTime;

// 解决：全局配置
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
spring.jackson.time-zone=Asia/Shanghai
```

### 11.2 枚举序列化问题

```java
// 问题：枚举序列化为索引
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Status {
    ACTIVE, INACTIVE
}

// 解决：全局配置
spring.jackson.serialization.write-enums-using-to-string=true
```

### 11.3 循环引用问题

```java
// 解决循环引用
@JsonIgnoreProperties({"parent"})
public class Child {
    @JsonManagedReference
    private Parent parent;
}

@JsonBackReference
public class Parent {
    @JsonIgnoreProperties({"children"})
    private List<Child> children;
}
```
```

## 3. 使用 Jackson2ObjectMapperBuilder（推荐的自定义方式）

这种方式可以复用Spring Boot的默认配置，只添加额外的定制：

```java
@Configuration
public class JacksonConfig {
    
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder
                // 继承Spring Boot的默认配置
                .createXmlMapper(false)
                .build()
                // 添加自定义配置
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new JavaTimeModule());
    }
}
```

## 4. 定制 Jackson2ObjectMapperBuilder

如果需要全局定制Jackson的构建方式：

```java
@Configuration
public class JacksonConfig {
    
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .dateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                .timeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                .locale(Locale.CHINA)
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .featuresToDisable(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                )
                .featuresToEnable(
                    DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                    JsonParser.Feature.ALLOW_COMMENTS
                )
                .modules(new JavaTimeModule(), createCustomModule());
    }
}
```

## 5. 针对特定场景的配置

### 5.1 审计组件专用配置

```java
@Configuration
public class AuditJacksonConfig {
    
    @Bean
    @Qualifier("auditObjectMapper")
    public ObjectMapper auditObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        
        // 审计专用配置
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 敏感数据处理
        mapper.addMixIn(String.class, SensitiveDataMixin.class);
        
        return mapper;
    }
}
```

### 5.2 API响应专用配置

```java
@Configuration
public class ApiJacksonConfig {
    
    @Bean
    @Qualifier("apiObjectMapper")
    public ObjectMapper apiObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        
        // API响应专用配置
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        return mapper;
    }
}
```

## 12. 在审计组件中的应用

### 12.1 审计组件配置示例

我们的审计组件(`AuditBeanPostProcessor`)已经优化为使用Spring Boot默认的ObjectMapper：

```java
@Component
@RequiredArgsConstructor
public class AuditBeanPostProcessor implements BeanPostProcessor {
    
    private final ObjectMapper objectMapper; // 自动注入Spring Boot默认的ObjectMapper
    
    // 序列化审计数据时使用
    private String serializeParameters(Object[] args) {
        try {
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            return "[序列化失败: " + e.getMessage() + "]";
        }
    }
}
```

### 12.2 审计推荐配置

针对审计组件的特殊需求，建议在`application.yml`中添加以下配置：

```yaml
spring:
  jackson:
    # 审计数据时间精度配置
    date-format: yyyy-MM-dd HH:mm:ss.SSS
    time-zone: Asia/Shanghai
    
    # 审计数据序列化配置
    serialization:
      write-dates-as-timestamps: false    # 使用可读的日期格式
      fail-on-empty-beans: false          # 允许序列化空对象
      write-null-map-values: false        # 不序列化null的Map值
      indent-output: false                # 压缩输出，减少存储空间
      
    # 审计数据反序列化配置（用于历史数据查询）
    deserialization:
      fail-on-unknown-properties: false   # 兼容历史数据格式变化
      accept-empty-string-as-null-object: true
      
    # 只包含非null值，减少审计数据大小
    default-property-inclusion: NON_NULL
```

### 12.3 敏感数据处理配置

结合数据脱敏功能，可以配置敏感数据的序列化方式：

```java
@JsonComponent
public class AuditJsonComponents {
    
    // 敏感字符串序列化器
    public static class SensitiveStringSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) 
                throws IOException {
            // 使用审计组件的数据脱敏工具
            String maskedValue = DataMaskingUtils.smartMask(value);
            gen.writeString(maskedValue);
        }
    }
}
```

### 12.4 审计数据查询配置

如果需要查询历史审计数据，建议配置宽松的反序列化策略：

```yaml
spring:
  jackson:
    deserialization:
      fail-on-unknown-properties: false     # 容忍字段变化
      fail-on-null-for-primitives: false    # 容忍数据类型变化
      accept-single-value-as-array: true    # 容忍数据结构变化
```

## 13. 配置验证

### 13.1 验证配置是否生效

```java
@RestController
public class ConfigTestController {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @GetMapping("/test/jackson-config")
    public Map<String, Object> testJacksonConfig() throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // 测试日期格式
        TestData data = new TestData();
        data.setCreateTime(LocalDateTime.now());
        data.setNullValue(null);
        
        String json = objectMapper.writeValueAsString(data);
        result.put("serialized", json);
        
        // 测试反序列化
        TestData deserialized = objectMapper.readValue(json, TestData.class);
        result.put("deserialized", deserialized);
        
        return result;
    }
    
    @Data
    public static class TestData {
        private LocalDateTime createTime;
        private String nullValue;
        private String normalValue = "test";
    }
}
```

### 13.2 审计组件测试

```java
@SpringBootTest
class AuditConfigTest {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testAuditDataSerialization() throws Exception {
        AuditEvent event = new AuditEvent();
        event.setOperation("测试操作");
        event.setCreateTime(LocalDateTime.now());
        event.setParameters(new Object[]{"param1", null, 123});
        
        String json = objectMapper.writeValueAsString(event);
        
        // 验证配置是否生效
        assertThat(json).doesNotContain("null");  // NON_NULL配置
        assertThat(json).contains("yyyy-MM-dd");   // 日期格式配置
    }
}
```

## 14. 总结和最佳实践

### 14.1 配置选择建议

1. **简单场景**：使用`application.yml`中的`spring.jackson`配置
2. **复杂场景**：使用`Jackson2ObjectMapperBuilder`进行定制
3. **特殊需求**：创建专用的ObjectMapper Bean

### 14.2 审计组件最佳实践

1. **使用默认ObjectMapper**：避免配置冲突，保持一致性
2. **合理设置包含策略**：使用`NON_NULL`减少审计数据大小
3. **配置容错性**：设置宽松的反序列化策略，兼容数据变化
4. **时间精度配置**：根据审计需求设置合适的时间格式
5. **敏感数据处理**：结合数据脱敏功能保护隐私

### 14.3 性能优化建议

1. **禁用不必要的特性**：如自动检测等
2. **使用紧凑输出**：生产环境关闭格式化
3. **合理设置缓存**：启用类型信息缓存
4. **异步处理**：审计数据序列化放在异步线程中

### 14.4 故障排查

1. **启用调试日志**：`logging.level.com.fasterxml.jackson=DEBUG`
2. **验证配置生效**：通过测试接口验证序列化结果
3. **检查Bean冲突**：确保没有多个ObjectMapper Bean
4. **版本兼容性**：确保Jackson版本与Spring Boot版本兼容

## 15. 参考资源

- [Spring Boot Jackson配置官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.json.jackson)
- [Jackson官方文档](https://github.com/FasterXML/jackson-docs)
- [Jackson配置参考](https://github.com/FasterXML/jackson-databind/wiki)
- [Spring Boot配置属性参考](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.json)

---

通过以上配置，您可以灵活地定制Spring Boot中ObjectMapper的行为，使其适应不同的业务需求。在审计组件中，我们推荐使用Spring Boot的默认ObjectMapper配置，通过`application.yml`进行必要的定制，这样既保持了配置的简洁性，又确保了与应用整体JSON处理的一致性。