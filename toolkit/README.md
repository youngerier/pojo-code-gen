# Toolkit 企业应用开发工具包

## 📋 概述

toolkit 模块是一个专为企业级Java应用开发设计的工具包，提供了一系列经过生产验证的工具类、规范类和最佳实践实现。该模块遵循Spring Boot生态标准，集成了现代Java开发中的常用组件和模式。

## 🎯 设计目标

- **开箱即用** - 提供企业应用开发中的常用功能组件
- **标准化** - 统一的编码规范和最佳实践
- **可扩展** - 模块化设计，支持自定义扩展
- **生产就绪** - 经过生产环境验证的实现
- **国际化友好** - 支持多语言和本地化

## 🏗️ 模块结构

```
toolkit/
├── src/main/java/com/abc/
│   ├── config/                    # 配置类
│   │   └── MybatisConfig.java    # MyBatis配置
│   └── web/support/              # Web支持组件
│       ├── enums/                # 枚举类
│       ├── exception/            # 异常体系
│       ├── audit/                # 统一审计组件
│       │   ├── processor/        # BeanPostProcessor实现
│       │   ├── example/          # 使用示例
│       │   ├── AuditEvent.java   # 审计事件模型
│       │   ├── Auditable.java    # 审计注解
│       │   ├── SensitiveParam.java # 敏感参数注解
│       │   ├── IgnoreParam.java  # 忽略参数注解
│       │   └── DataMaskingUtils.java # 数据脱敏工具
│       └── ...                   # 其他支持组件
└── docs/                         # 文档目录
    ├── 统一审计组件设计文档.md
    ├── 参数注解使用指南.md
    ├── 异常体系设计文档.md
    └── ...                       # 其他文档
```

## 🚀 核心功能

### 🔍 1. 统一审计组件 (重点特性)

基于 **BeanPostProcessor** 的统一审计实现方案，提供完整的操作审计功能。

**主要特性：**
- 🎯 **统一实现**：单一的 BeanPostProcessor + MethodInterceptor 方案
- 🚀 **高性能**：异步审计，CGLIB代理，Bean缓存优化
- 🔒 **数据安全**：多种脱敏策略，参数注解支持
- 🎨 **使用简单**：注解驱动，零配置启动
- 🔄 **向前兼容**：支持传统索引和新参数注解方式

**脱敏策略：**
- `DEFAULT`: 保留前2后2位 (`ab****yz`)
- `FULL`: 完全脱敏 (`****`)
- `EMAIL`: 邮箱脱敏 (`a***@example.com`)
- `PHONE`: 手机号脱敏 (`138****5678`)
- `BANK_CARD`: 银行卡脱敏 (`**** **** **** 1234`)
- `ID_CARD`: 身份证脱敏 (`1234**********5678`)
- `CUSTOM`: 自定义SpEL表达式

### 🚨 2. 异常处理体系
- 统一异常基类设计
- 国际化异常消息支持
- 全局异常处理器
- 断言工具类
- 异常工具类

### 📄 3. 响应规范
- 统一响应格式
- 分页查询支持
- 排序和过滤
- 错误码规范

### ✅ 4. 数据验证（计划）
- 参数验证框架
- 自定义验证器
- 业务规则验证
- 数据完整性检查

### 🔐 5. 安全组件（计划）
- 认证授权工具
- 密码加密工具
- 安全配置
- 审计日志

### 🛠️ 6. 工具类库（计划）
- 日期时间工具
- 字符串处理工具
- 集合操作工具
- JSON处理工具
- 文件操作工具
- 网络请求工具

### 💾 7. 缓存支持（计划）
- 缓存抽象层
- 分布式缓存工具
- 缓存策略配置
- 缓存监控

## 📚 文档导航

### 已完成文档
- [异常体系设计文档](./异常体系设计文档.md) - 详细介绍异常处理体系的设计和使用
- [审计组件设计文档](./docs/审计组件设计文档.md) - 企业级审计组件的设计和实现

### 计划文档
- [工具类使用指南](./docs/工具类使用指南.md) - 各种工具类的使用方法和示例
- [验证框架设计文档](./docs/验证框架设计文档.md) - 数据验证框架的设计和实现
- [安全组件设计文档](./docs/安全组件设计文档.md) - 安全相关组件的设计和配置
- [缓存组件设计文档](./docs/缓存组件设计文档.md) - 缓存组件的设计和使用
- [最佳实践指南](./docs/最佳实践指南.md) - 企业应用开发的最佳实践

## 🛠️ 技术栈

- **Java 17** - 核心语言版本
- **Spring Framework 6.2.7** - 基础框架
- **Spring Boot** - 自动配置和启动器
- **MyBatis Flex 1.10.0** - ORM框架
- **Lombok 1.18.24** - 简化Java开发
- **MapStruct 1.5.5.Final** - 对象映射
- **SLF4J + Logback** - 日志框架
- **Validation API 3.0.2** - 验证框架

## 🚀 快速开始

### 1. 添加依赖

在您的项目中添加toolkit依赖：

``xml
<dependency>
    <groupId>io.github.youngerier</groupId>
    <artifactId>toolkit</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置扫描

在Spring Boot应用中启用组件扫描：

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.yourpackage", "com.abc.web.support"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 配置国际化（可选）

``properties
# application.yml
spring:
  messages:
    basename: messages
    encoding: UTF-8
    cache-duration: 3600
```

## 📖 使用示例

### 🔍 统一审计组件 (重点功能)

**基础使用：**
```java
@Service
public class UserService {
    
    // 用户登录审计
    @Auditable(operation = "用户登录", eventType = AuditEventType.LOGIN)
    public boolean login(String username, 
                        @SensitiveParam(strategy = MaskStrategy.FULL) String password) {
        // 登录逻辑
        return authenticate(username, password);
    }
    
    // 用户注册审计 - 多种脱敏策略
    @Auditable(operation = "用户注册", eventType = AuditEventType.CREATE)
    public void register(@SensitiveParam(strategy = MaskStrategy.EMAIL) String email,
                        @SensitiveParam(strategy = MaskStrategy.PHONE) String phone,
                        @IgnoreParam(reason = "请求对象过大") Object request) {
        // 注册逻辑
    }
}
```

**类级别审计：**
```java
@Service
@Auditable(module = "用户服务", eventType = AuditEventType.BUSINESS_OPERATION)
public class UserService {
    
    public void method1() { /* 会被审计 */ }
    public void method2() { /* 会被审计 */ }
    
    @Auditable(operation = "特殊操作")  // 覆盖类级别配置
    public void specialMethod() { /* 使用方法级别配置 */ }
}
```

**条件审计和业务标识：**
```java
@Auditable(
    operation = "条件操作",
    condition = "#important == true",  // 只有重要操作才审计
    businessKey = "#userId",            // 业务标识
    async = true                        // 异步记录，不影响性能
)
public void conditionalOperation(String userId, boolean important) {
    // 业务逻辑
}
```

### 🚨 异常处理
```
@RestController
public class UserController {
    
    public Response<User> getUser(@PathVariable Long id) {
        // 参数验证
        ExceptionUtils.requireNonNull(id, "用户ID不能为空");
        
        // 业务逻辑
        User user = userService.findById(id);
        ExceptionUtils.throwBusinessIf(user == null, 
            I18nCommonExceptionCode.DATA_NOT_FOUND);
        
        return Response.success(user);
    }
}
```

### 📄 分页查询
```
public Response<Pagination<User>> queryUsers(UserQuery query) {
    // 使用分页查询
    Pagination<User> result = userService.queryUsers(query);
    return Response.success(result);
}
```

### 审计组件
```
// 使用注解进行自动审计
@Auditable(
    operation = "CREATE_USER",
    description = "创建用户",
    eventType = AuditEventType.DATA_OPERATION
)
public User createUser(CreateUserRequest request) {
    // 业务逻辑
}

// 手动记录审计事件
AuditUtils.recordLogin(userId, username, clientIp, success);

// 使用构建器模式
AuditUtils.builder()
    .userId(userId)
    .operation("UPDATE_USER")
    .eventType(AuditEventType.DATA_OPERATION)
    .success()
    .save();
```

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目基于 Apache License 2.0 许可证开源。

## 🔗 相关链接

- [pojo-code-gen 项目主页](../README.md)
- [代码生成器文档](../codegen-core/README.md)
- [Maven插件文档](../generator-maven-plugin/README.md)