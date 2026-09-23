# 从 `io.github.youngerier:toolkit` 迁移

`toolkit` 单体 artifact 已按职责拆分为多个模块。拆分是**坐标级**的破坏性变更，
但 Java 包名除一处例外（见下）保持不变，因此业务代码通常只需替换依赖坐标。

## 1. 坐标映射

| 旧坐标 | 新坐标 | 说明 |
|---|---|---|
| `io.github.youngerier:toolkit` | `io.github.youngerier:toolkit-core` | 统一响应体、断言、异常体系、消息格式化、枚举、反射工具、分页模型 |
| ↑ | `io.github.youngerier:toolkit-i18n` | 异常消息国际化 |
| ↑ | `io.github.youngerier:toolkit-trace` | 链路 ID（原 `support.trace`） |
| ↑ | `io.github.youngerier:toolkit-mybatis-flex` | `QueryWrapperHelper` |
| ↑ | `io.github.youngerier:toolkit-office` | Excel 导出（原 `support.office`） |
| ↑ | `io.github.youngerier:toolkit-web` | 全局异常处理器与 Spring Security 401/403 |
| ↑ | `io.github.youngerier:toolkit-rocketmq` | RocketMQ 集成 |
| — | `io.github.youngerier:toolkit-bom` | 统一版本 |
| — | `io.github.youngerier:toolkit-spring-boot-starter` | 聚合 `core + i18n + trace + web` |

推荐做法：导入 `toolkit-bom`，然后按需声明模块，不必再写版本号。

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.youngerier</groupId>
            <artifactId>toolkit-bom</artifactId>
            <version>${toolkit.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.youngerier</groupId>
        <artifactId>toolkit-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

## 2. 依赖声明方式的变化（重要）

旧 `toolkit` 把 `easyexcel`、`commons-lang3`、`swagger-annotations-jakarta`、
`mybatis-flex-core`、`rocketmq-*`、`spring-security-core` 全部声明为 `optional`，
使用方必须自己补齐，否则在运行期才会踩到 `NoClassDefFoundError`。

新模块把各自**真正需要**的依赖声明为必需：

| 模块 | 现在会自动传递 |
|---|---|
| `toolkit-office` | `easyexcel`、`commons-lang3`、`swagger-annotations-jakarta` |
| `toolkit-mybatis-flex` | `mybatis-flex-core` |
| `toolkit-rocketmq` | `rocketmq-client`、`rocketmq-spring-boot` |
| `toolkit-web` | `spring-web`、`spring-webmvc`（`spring-security-core` 仍为可选） |

因此你**可以删掉**为了绕过旧问题而手工添加的重复依赖声明。

## 3. 唯一的包名变更

生成的 Repository 会调用排序适配方法，该类移到了独立模块的新包下：

```
- io.github.youngerier.support.page.QueryWrapperHelper
+ io.github.youngerier.support.page.flex.QueryWrapperHelper
```

**影响**：手写代码里直接引用该类的需要改 import；
**更重要的是已生成的代码需要重新生成**（升级 `generator-maven-plugin` 后重新执行生成即可）。
分页模型本身（`Pagination`、`IPagination`、`AbstractPageQuery`、`QueryOrderField`）包名不变。

自动装配类同时移到了各模块自己的包下（这些不是公开 API，仅列出便于排查）：

```
- io.github.youngerier.support.autoconfigure.WebSupportAutoConfiguration
+ io.github.youngerier.support.web.autoconfigure.WebSupportAutoConfiguration
- io.github.youngerier.support.autoconfigure.RocketMqAutoConfiguration
+ io.github.youngerier.support.rocketmq.autoconfigure.RocketMqAutoConfiguration
+ io.github.youngerier.support.trace.autoconfigure.TraceAutoConfiguration   （新增）
```

## 4. 行为变更清单

以下变更都是修复，但会让运行期可观测的行为发生变化，请对照检查：

| 变更 | 之前 | 现在 |
|---|---|---|
| 启动安全性 | classpath 有 `rocketmq-spring-boot` 但未配 `rocketmq.name-server` 时，`RocketMqProducer` 注入失败导致**应用启动失败** | 增加 `@ConditionalOnBean(RocketMQTemplate.class)`，不再装配 |
| 启动安全性 | 无 `spring-security` 且自动配置以反射方式加载时，抛 `NoClassDefFoundError` 导致**应用启动失败** | 条件移到嵌套配置类的类级别并用 `name` 形式，两条路径都安全 |
| 非法分页参数 | `queryPage=0` / `querySize=0` 透传到 ORM 抛异常，最终 **HTTP 500** | 绑定期即拒绝，返回 **400** |
| `pageNumber` 别名 | 直接赋值，**绕过**页大小校验 | 与 `queryPage` 共用同一套校验 |
| 非法排序字段 | 原样拼进 `ORDER BY`（自定义 `QueryOrderField` 时可 SQL 注入） | 标识符白名单校验，非法字段返回 400 |
| SpEL 取值表达式 | `StandardEvaluationContext`，允许 `T(...)`/静态方法/构造器 | 只读数据绑定上下文，上述写法被拒绝；表达式改为构建期解析 |
| traceId 来源 | 上游 header / MQ 属性只 `trim()` 后透传 | 白名单 `[A-Za-z0-9._-]{1,128}` 校验，非法则重新生成 |
| MDC 清理 | 只 `MDC.remove("traceId")`，会误删上游 agent 写入的 traceId，并让业务 MDC 键串到下一个请求 | 快照 + 还原，只影响本库自己的键 |
| 异步任务 traceId | `MdcTaskDecorator` 从未装配，`@Async` 丢失 traceId | 由 `TraceAutoConfiguration` 装配为容器内唯一的 `TaskDecorator`（可用 `youngerier.trace.propagate-to-async=false` 关闭） |
| 消费端 MDC | 结束只删 traceId | 还原到消费前状态 |
| 发送端 MDC | 无 traceId 时生成并**写回调用线程 MDC**，池化线程后续消息共用同一 id | 只读，缺失时就地生成、不写回 |
| 导出任务终态 | 只有 `COMPLETED` 会 `finish()`，失败/取消路径不释放 workbook | 失败/中断/取消调用 `abort()` 释放资源 |
| 导出文件 | `finish()` 未调用 `ExcelWriter.finish()`，文件可能不完整 | 已补上 |
| 取数异常 | `fetch` 返回 null → NPE；忽略 `page` 的实现 → 死循环 | 明确异常 + 翻页安全上限（`MAX_FETCH_PAGES`） |
| Office 打印 | 枚举打印 `name()`、集合打印 `[a, b]`、`java.util.Date` 抛 `ClassCastException` | 修正 `isAssignableFrom` 方向，`Date` 使用专用 formatter |
| 导出表头 | 空标题被过滤导致表头整体左移一列；cells 模式表头被当数据转置 | 表头列数严格对齐，且不参与转置 |
| HTTP 错误映射 | 兜底 `@ExceptionHandler(Exception.class)` 把 `ResponseStatusException`、415、406、503 一律吞成 500 | 按 `ErrorResponse` 的语义返回真实状态码，405 补 `Allow` 头 |

## 5. 迁移步骤

1. 升级 `generator-maven-plugin` 与 `toolkit-*` 到新版本。
2. 用 `toolkit-bom` 替换原来逐个声明的版本；把 `toolkit` 依赖换成所需模块（或先用 starter）。
3. 删除为绕开旧 `optional` 问题而手工添加的 `easyexcel` / `commons-lang3` / `swagger-annotations-jakarta` 声明。
4. 重新执行代码生成，让生成的 Repository 引用新的 `QueryWrapperHelper` 包路径。
5. 全局搜索并修正 `io.github.youngerier.support.page.QueryWrapperHelper` 的 import。
6. 按第 4 节清单核对受影响的接口契约（尤其是错误状态码、非法分页参数、Excel 表头列序）。
