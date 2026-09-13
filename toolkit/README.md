# Toolkit 基础开发工具包

Spring Boot Web 项目的统一基础设施：**统一响应、统一异常、链路 trace、分页、操作审计、i18n**。
基于 Spring Boot 3.x / Spring Framework 6.x / Java 17。

## 快速开始

```xml
<dependency>
    <groupId>io.github.youngerier</groupId>
    <artifactId>toolkit</artifactId>
    <version>1.0.5</version>
</dependency>
```

**无需任何配置**：工具包通过 Spring Boot 自动装配生效（`AutoConfiguration.imports`），
引入依赖后全局异常处理器和 traceId 过滤器即已启用。

在日志 pattern 中加入 `%X{traceId}` 即可输出链路 ID，例如：

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
```

## 核心功能

### 1. 统一响应 Response

```java
Response.ok(data);                    // {code:200, message:"success", data:...}
Response.error("操作失败");            // {code:500, message:"操作失败"}
Response.error(ExceptionCode);       // code 取异常码，message 取异常码描述
```

### 2. 统一异常处理

业务代码直接抛 `BaseException`，`GlobalExceptionHandler` 自动转成 `Response`：

```java
throw BaseException.badRequest("参数错误");
throw BaseException.notFound("用户不存在");
throw AssertUtils...                 // AssertUtils 断言失败同样抛 BaseException
```

- 异常码即 HTTP 状态码（400/401/403/404/429/500），见 `DefaultExceptionCode`
- `@Valid` 参数校验失败、JSON 解析错误、404、405 均有统一处理
- 未捕获异常兜底 500，详情只记日志、不泄漏给调用方
- `BaseException.friendly(msg)` 对用户只返回"业务异常，请稍后重试"
- 日志级别由 `ExceptionLogLevel` 控制（NONE/INFO/WARN/ERROR）

### 3. 链路 trace

`TraceIdFilter` 为每个请求：
- 优先复用上游 `X-Request-ID` 请求头，没有则生成；
- 放入 SLF4J MDC（key 为 `traceId`）并回写响应头；
- 审计线程池通过 `MdcTaskDecorator` 透传 traceId。

配置项：`youngerier.trace.enabled=false` 可关闭。

### 4. 分页

```java
// 入参：继承 AbstractPageQuery
public class UserQuery extends AbstractPageQuery<DefaultOrderField> {
    private String username;
}

// 出参
Pagination<User> page = Pagination.of(records, query, total);
Pagination<UserDTO> dtoPage = Pagination.convert(page, userConverter::toDto);
```

### 5. 操作审计

方法或类上加 `@Auditable` 即可，默认异步输出到日志：

```java
@Auditable(operation = "创建用户", businessKey = "#user.id")
public void create(User user) { ... }

@Auditable(operation = "登录")
public void login(String username,
                  @SensitiveParam(strategy = MaskStrategy.FULL) String password,
                  @IgnoreParam String rawToken) { ... }

// 仅当条件成立时审计
@Auditable(operation = "重要操作", condition = "#important")
public void op(boolean important) { ... }
```

- SpEL 可直接引用**真实参数名**（需 `-parameters`，本项目已开启）或 `#param0`
- 默认提供审计线程池（traceId 透传），可用名为 `auditExecutor` 的 Bean 覆盖
- 需要落库/上报时实现单方法接口 `AuditSink` 注册为 Bean 即可覆盖默认日志实现
- 当前操作人通过实现 `AuditUserProvider` 接入
- 配置项：`youngerier.audit.enabled=false` 可关闭

### 6. i18n

`SpringI18nMessageUtils.getMessage("$.user.notfound")` 解析 `$.` 前缀的消息键，
自动装配会接入容器中的 `MessageSource` 并使用请求 Locale。

### 7. Excel 支持（可选）

`io.github.youngerier.support.office` 包提供基于 EasyExcel 的导出能力，
EasyExcel 为 optional 依赖，使用时需自行引入：

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
</dependency>
```

## 包结构

```
io.github.youngerier.support
├── Response.java              # 统一响应
├── AssertUtils.java           # 业务断言
├── web/                       # GlobalExceptionHandler
├── trace/                     # traceId Filter / MDC / 异步透传
├── exception/                 # BaseException / ExceptionCode / DefaultExceptionCode
├── page/                      # AbstractPageQuery / Pagination / QueryWrapperHelper
├── enums/                     # 通用枚举
├── audit/                     # @Auditable 审计切面、脱敏、AuditSink SPI
├── i18n/ message/             # 国际化与消息格式化
└── office/                    # Excel 导出（optional）
```
