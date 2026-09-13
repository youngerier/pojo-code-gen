# Toolkit 基础开发工具包

Spring Boot Web 项目的统一基础设施：**统一响应、统一异常、链路 trace、分页**。
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

toolkit 只强依赖 spring-context / validation-api / slf4j；Web、MyBatis-Flex 均为 optional，
由使用方项目自带（Boot Web 项目通常已具备）。按需补充：

| 需要的能力 | 额外依赖 |
|---|---|
| 分页 SQL 包装 `QueryWrapperHelper` | `com.mybatis-flex:mybatis-flex-core` |
| Excel 导出 `support.office` | `com.alibaba:easyexcel`、`org.apache.commons:commons-lang3` |

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

链路 traceId 不放在响应体中，通过响应头 `X-Request-ID` 返回。

### 2. 统一异常处理

业务代码直接抛 `BaseException`，`GlobalExceptionHandler` 自动转成 `Response`：

```java
throw BaseException.badRequest("参数错误");
throw BaseException.notFound("用户不存在");
throw BaseException.conflict("名称已存在");
throw AssertUtils.isTrue(...);       // AssertUtils 断言失败同样抛 BaseException
```

内置异常码（`DefaultExceptionCode`，code 即 HTTP 状态码）：

| 工厂方法 | code | 含义 |
|---|---|---|
| `badRequest` | 400 | 请求不合法 |
| `unauthorized` | 401 | 未认证或登录已过期 |
| `forbidden` | 403 | 无权限 |
| `notFound` | 404 | 资源不存在 |
| `conflict` | 409 | 资源冲突 |
| `serviceUnavailable` | 503 | 服务暂不可用 |
| `tooManyRequests` | 429 | 请求过于频繁 |
| `common` | 500 | 系统内部错误 |

其他特性：

- 消息支持 slf4j 风格占位符：`BaseException.notFound("用户 {} 不存在", id)`
- **自定义业务码**：实现 `ExceptionCode` 枚举（如 code=10001），通过 `BaseException.business(code, msg)` 抛出，
  响应 HTTP 状态为 422，响应体保留原始业务码
- `@Valid` 校验失败、缺参、参数类型错误、JSON 解析错误、404、405、上传超限均有统一处理
- classpath 存在 Spring Security 时自动处理 `AuthenticationException`(401) / `AccessDeniedException`(403)
- `BaseException.friendly(msg)` 对调用方只返回通用提示（"系统繁忙，请稍后重试"），真实信息只记日志
- 未捕获异常兜底 500，详情只进日志、不泄漏给调用方
- 4xx/业务异常默认 WARN 单行日志不打堆栈，5xx 默认 ERROR 完整堆栈；
  可用 `ExceptionLogLevel`（NONE/INFO/WARN/ERROR）按异常调整

#### 国际化（i18n）

异常支持两种消息模式，按场景选择：

**1. 字面量**（内部系统、快速开发，消息不做翻译）：

```java
throw BaseException.notFound("用户不存在");
throw BaseException.notFound("用户 {} 不存在", id);
```

**2. i18n 模式**（对外系统，推荐）：**异常码枚举只需实现 `ExceptionCode` 接口即自动获得多语言能力**，
消息键按约定自动推导（`USER_NOT_FOUND` → `exception.user_not_found`），无需覆写任何方法。

```java
// 1) 业务异常码：实现接口即可，getMessageKey() 按枚举名自动生成
public enum UserErrorCode implements ExceptionCode {
    USER_NOT_FOUND("10001", "用户不存在");

    private final String code;
    private final String desc;
    UserErrorCode(String code, String desc) { this.code = code; this.desc = desc; }
    public String getCode() { return code; }
    public String getDesc() { return desc; }
}

// 2) 抛出时只传码与参数（MessageFormat 风格 {0}）
throw BaseException.i18n(UserErrorCode.USER_NOT_FOUND, id);
```

消息来源按顺序解析，任一来源命中即返回：

1. 应用的 Spring `MessageSource`（如 `messages.properties` / `messages_en.properties`）
2. 所有 `ExceptionMessageProvider` Bean（**典型用于从数据库字典表加载**）
3. toolkit 内置的中英文 bundle（9 个默认异常码 400/401/403/404/409/413/429/500/503，零配置）
4. 异常码的 `desc` 兜底

properties 方式：

```properties
# messages.properties
exception.user_not_found=用户 {0} 不存在
# messages_en.properties
exception.user_not_found=User {0} not found
```

**数据库方式**：实现一个 Bean 即可（建议加缓存）：

```java
@Component
@RequiredArgsConstructor
public class DbExceptionMessageProvider implements ExceptionMessageProvider {

    private final ErrorMessageMapper mapper;

    @Override
    public String getMessage(String key, Locale locale, Object... args) {
        String template = mapper.findMessage(key, locale.toLanguageTag()); // 查库
        if (template == null) {
            return null;                                  // 返回 null 继续尝试后续来源
        }
        return args.length == 0 ? template
                : new MessageFormat(template, locale).format(args);
    }
}
```

请求头 `Accept-Language: en-US` 时响应 `User 42 not found`，中文环境返回 `用户 42 不存在`。
非枚举实现（如动态构造的异常码）可覆写 `getMessageKey()` 指定键。

### 3. 链路 trace

`TraceIdFilter` 为每个请求：
- 优先复用上游 `X-Request-ID` 请求头，没有则生成；
- 放入 SLF4J MDC（key 为 `traceId`）并回写同名响应头；
- `MdcTaskDecorator` 可装饰线程池实现异步透传。

非 HTTP 入口（定时任务、消息消费者）可手动埋点：

```java
TraceContext.ensureTraceId();              // 没有 traceId 时生成
executor.execute(TraceContext.wrap(task)); // 手动透传 MDC
```

配置项：

| 配置 | 默认值 | 说明 |
|---|---|---|
| `youngerier.trace.enabled` | true | 关闭过滤器 |
| `youngerier.trace.header-name` | X-Request-ID | traceId 请求/响应头名 |

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

入参同时支持 `queryPage/querySize` 与主流命名 `pageNumber/pageSize`；
排序通过 `orderFields` + `orderTypes` 两个平行数组按位置一一对应，长度不一致时忽略排序。

### 5. Excel 支持（可选）

`io.github.youngerier.support.office` 包提供基于 EasyExcel 的导入导出能力，
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
├── web/                       # GlobalExceptionHandler / SecurityExceptionHandler
├── trace/                     # traceId Filter / MDC / 异步透传
├── exception/                 # BaseException / ExceptionCode / DefaultExceptionCode
├── page/                      # AbstractPageQuery / Pagination / QueryWrapperHelper
├── enums/                     # 通用枚举
├── message/                   # 消息占位符格式化（slf4j 风格）
├── i18n/                      # 异常消息多语言解析（内置中英文 bundle）
├── constants/                 # 常量
├── util/                      # 反射等工具
├── autoconfigure/             # Spring Boot 自动装配
└── office/                    # Excel 导入导出（optional）
```
