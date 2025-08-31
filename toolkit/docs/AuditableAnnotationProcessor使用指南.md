# AuditableAnnotationProcessor 使用指南

## 📋 概述

[AuditableAnnotationProcessor](src/main/java/com/abc/web/support/audit/processor/AuditableAnnotationProcessor.java) 是一个编译时注解处理器，用于在编译期间验证 `@Auditable` 注解的正确使用。它能够：

- 验证 `@Auditable` 注解的参数配置
- 检查敏感参数索引的有效性
- 提供编译时警告和错误提示
- 生成静态检查报告

## 🔧 配置方法

### 1. 自动发现配置

注解处理器已通过 `META-INF/services/javax.annotation.processing.Processor` 文件自动配置：

```
com.abc.web.support.audit.processor.AuditableAnnotationProcessor
```

### 2. Maven 配置

在项目的 `pom.xml` 中，注解处理器会自动生效。如果需要显式配置：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>17</source>
        <target>17</target>
        <annotationProcessorPaths>
            <!-- 其他注解处理器 -->
            <path>
                <groupId>io.github.youngerier</groupId>
                <artifactId>toolkit</artifactId>
                <version>1.0.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### 3. IDE 配置

在 IntelliJ IDEA 中：
1. 打开 `Settings` -> `Build, Execution, Deployment` -> `Compiler` -> `Annotation Processors`
2. 勾选 `Enable annotation processing`
3. 确保 `Obtain processors from project classpath` 被选中

## 🚀 使用示例

### 1. 正确的注解使用

```java
@Service
public class UserService {
    
    // ✅ 正确：指定了操作名称
    @Auditable(
        operation = "CREATE_USER",
        description = "创建用户",
        eventType = AuditEventType.DATA_OPERATION
    )
    public User createUser(String username, String email) {
        // 业务逻辑
        return new User(username, email);
    }
    
    // ✅ 正确：敏感参数索引有效
    @Auditable(
        operation = "UPDATE_PASSWORD",
        description = "更新密码",
        sensitiveParams = {1}  // 第二个参数是密码
    )
    public void updatePassword(Long userId, String newPassword) {
        // 业务逻辑
    }
}
```

### 2. 会触发警告的使用

```java
@Service
public class UserService {
    
    // ⚠️ 警告：未指定operation属性
    @Auditable(description = "创建用户")
    public User createUser(String username, String email) {
        return new User(username, email);
    }
}
```

编译时输出：
```
警告: 建议为@Auditable注解指定operation属性
```

### 3. 会触发错误的使用

```java
@Service
public class UserService {
    
    // ❌ 错误：敏感参数索引超出范围
    @Auditable(
        operation = "UPDATE_USER",
        sensitiveParams = {5}  // 只有2个参数，索引5无效
    )
    public void updateUser(Long userId, String username) {
        // 业务逻辑
    }
}
```

编译时输出：
```
错误: 敏感参数索引 5 超出参数范围 [0, 2)
```

### 4. 类级别注解

```java
// ℹ️ 信息：类级别注解会对所有公共方法生效
@Auditable(
    operation = "USER_OPERATION",
    eventType = AuditEventType.BUSINESS_OPERATION
)
@Service
public class UserService {
    
    public User createUser(String username) {
        return new User(username);
    }
    
    public void deleteUser(Long id) {
        // 删除逻辑
    }
}
```

编译时输出：
```
注意: 检测到类级别的@Auditable注解，将对所有公共方法进行审计
```

## 📊 验证规则

### 1. 操作名称验证
- **规则**: 建议为 `@Auditable` 注解指定 `operation` 属性
- **级别**: WARNING
- **说明**: 虽然不是必需的，但指定操作名称有助于更好地识别审计事件

### 2. 敏感参数索引验证
- **规则**: `sensitiveParams` 中的索引必须在方法参数范围内
- **级别**: ERROR
- **说明**: 索引从0开始，不能超过方法参数的数量

### 3. 忽略参数索引验证
- **规则**: `ignoreParams` 中的索引必须在方法参数范围内
- **级别**: ERROR
- **说明**: 与敏感参数验证类似

### 4. 类级别注解提醒
- **规则**: 检测类级别的 `@Auditable` 注解
- **级别**: NOTE
- **说明**: 提醒开发者类级别注解会影响所有公共方法

## 🔍 实际演示

让我们创建一个演示项目来看看注解处理器的实际效果：

### 步骤1：创建测试类

```java
// 演示文件：DemoService.java
@Service
public class DemoService {
    
    // 这会触发警告
    @Auditable(description = "创建用户")
    public User createUser(String username, String email) {
        return new User(username, email);
    }
    
    // 这会触发错误
    @Auditable(
        operation = "UPDATE_PASSWORD",
        sensitiveParams = {3}  // 错误：只有2个参数
    )
    public void updatePassword(Long userId, String password) {
        // 更新密码逻辑
    }
    
    // 这是正确的使用
    @Auditable(
        operation = "DELETE_USER",
        description = "删除用户",
        eventType = AuditEventType.DATA_OPERATION
    )
    public void deleteUser(Long userId) {
        // 删除逻辑
    }
}
```

### 步骤2：编译项目

```bash
mvn clean compile
```

### 步骤3：查看编译输出

```
[WARNING] 建议为@Auditable注解指定operation属性
  位置: com.example.DemoService.createUser(java.lang.String,java.lang.String)

[ERROR] 敏感参数索引 3 超出参数范围 [0, 2)
  位置: com.example.DemoService.updatePassword(java.lang.Long,java.lang.String)

[NOTE] 检测到方法级别的@Auditable注解
  位置: com.example.DemoService.deleteUser(java.lang.Long)
```

## ⚙️ 高级配置

### 1. 禁用注解处理器

如果需要临时禁用注解处理器，可以在编译时添加参数：

```bash
mvn compile -Dproc:none
```

### 2. 只运行注解处理器

```bash
mvn compile -Dproc:only
```

### 3. 详细输出

```bash
mvn compile -X
```

## 🧪 测试注解处理器

创建单元测试来验证注解处理器的功能：

```java
// 测试文件：AuditableAnnotationProcessorTest.java
public class AuditableAnnotationProcessorTest {
    
    @Test
    public void testValidAnnotation() {
        // 测试正确的注解使用
        // 应该编译成功，无警告无错误
    }
    
    @Test
    public void testMissingOperation() {
        // 测试缺少operation的注解
        // 应该产生警告
    }
    
    @Test
    public void testInvalidSensitiveParams() {
        // 测试无效的敏感参数索引
        // 应该产生编译错误
    }
}
```

## 📚 最佳实践

### 1. 开发建议
- **总是指定 `operation`**: 虽然不是必需的，但有助于审计日志的可读性
- **仔细检查参数索引**: 确保 `sensitiveParams` 和 `ignoreParams` 的索引正确
- **使用描述性的操作名称**: 如 `CREATE_USER`、`UPDATE_PASSWORD` 等

### 2. 团队协作
- **统一代码审查**: 利用注解处理器的检查结果进行代码审查
- **持续集成**: 在CI/CD流程中启用注解处理器检查
- **文档化**: 记录团队使用 `@Auditable` 注解的规范

### 3. 调试技巧
- **查看编译日志**: 注意编译时的警告和错误信息
- **IDE集成**: 利用IDE的注解处理器支持实时查看问题
- **分步调试**: 可以临时禁用注解处理器来排查编译问题

## 🔗 相关文档

- [审计组件设计文档](../审计组件设计文档.md)
- [Auditable注解使用指南](./Auditable注解使用指南.md)
- [Spring扩展机制审计对比](./Spring扩展机制审计对比.md)

## 📝 总结

`AuditableAnnotationProcessor` 为 `@Auditable` 注解提供了编译时验证功能，帮助开发者：

1. **提前发现问题**: 在编译期而不是运行期发现配置错误
2. **保证代码质量**: 通过静态检查确保注解使用的正确性
3. **提升开发效率**: 减少因配置错误导致的调试时间
4. **标准化使用**: 促进团队对审计注解的规范使用

通过合理使用这个注解处理器，可以大大提高审计功能的可靠性和开发效率。