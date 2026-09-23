# Java Code Generation Toolkit

一套面向「后端服务快速起盘」的通用支撑库 + 代码生成器。由两部分组成：

- **`codegen-core` / `generator-maven-plugin`**：从实体类生成 DTO、Request、Response、Query、Service、ServiceImpl、Repository、MapStruct 转换器。
- **`toolkit-*`**：生成代码与业务代码共同依赖的运行时支撑层（统一响应体、异常与 i18n、链路 ID、分页、Excel 导出、RocketMQ 集成）。

## 模块一览

按需引入，不要整体依赖。每个模块的依赖都是**诚实的**：用得到就声明为必需，不再靠 `optional` 把 `NoClassDefFoundError` 推到运行期。

| 模块 | 内容 | 关键依赖 | 什么时候引入 |
|---|---|---|---|
| `toolkit-core` | `Response`、`AssertUtils`、异常体系、消息格式化、枚举、反射工具、分页模型（`Pagination`/`AbstractPageQuery`） | `spring-core`、`jakarta.validation-api`、`slf4j-api` | 几乎总是 |
| `toolkit-i18n` | 异常消息解析链（应用 `MessageSource` → 自定义 Provider → 内置中英文 bundle → 异常码兜底） | `spring-context` | 需要多语言错误文案 |
| `toolkit-trace` | MDC 上下文、跨线程池复制、HTTP 入站过滤器、跨进程载体抽象 | `slf4j-api`、`spring-core`；`spring-web`/`spring-boot` 可选 | 需要 traceId 贯穿日志 |
| `toolkit-mybatis-flex` | `QueryWrapperHelper`：分页参数 → 带 ORDER BY 的 `QueryWrapper` | `mybatis-flex-core` | 用生成的 Repository |
| `toolkit-office` | Excel 导出与模板渲染（描述符、受限 SpEL 取值、分页导出任务） | `easyexcel`、`commons-lang3`、`swagger-annotations-jakarta` | 需要导出 Excel |
| `toolkit-web` | `GlobalExceptionHandler`、Spring Security 401/403、自动装配 | `spring-web`、`spring-webmvc`；`spring-security-core` 可选 | Web 服务 |
| `toolkit-rocketmq` | 链路透传 Hook、消费端基类、信封式发送封装 | `rocketmq-client`、`rocketmq-spring-boot` | 使用 RocketMQ |
| `toolkit-spring-boot-starter` | 聚合 `core + i18n + trace + web` | — | 只想加一行依赖 |
| `toolkit-bom` | 统一版本管理 | — | 引入了 2 个以上模块 |

## 快速开始

先导入 BOM，版本一次对齐：

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
```

然后按需引入（Web 场景可用 starter 一行搞定）：

```xml
<!-- 常用组合：core + i18n + trace + web -->
<dependency>
    <groupId>io.github.youngerier</groupId>
    <artifactId>toolkit-spring-boot-starter</artifactId>
</dependency>

<!-- 用生成的 Repository 时追加 -->
<dependency>
    <groupId>io.github.youngerier</groupId>
    <artifactId>toolkit-mybatis-flex</artifactId>
</dependency>

<!-- 需要 Excel 导出时追加（easyexcel / commons-lang3 / swagger-annotations 会自动带出） -->
<dependency>
    <groupId>io.github.youngerier</groupId>
    <artifactId>toolkit-office</artifactId>
</dependency>
```

从旧坐标 `io.github.youngerier:toolkit` 迁移请见 [MIGRATION.md](MIGRATION.md)。

## 统一响应体与异常

```java
// 成功
return Response.ok(userDto);

// 业务异常：要么字面量，要么只带异常码与参数，由边缘按请求 Locale 解析
throw BaseException.notFound("用户 {} 不存在", id);
throw BaseException.i18n(UserErrorCode.USER_NOT_FOUND, id);

// 不想向调用方暴露内部细节
throw BaseException.friendly("下游返回了非预期结构");
```

异常码实现 `ExceptionCode` 即可；以枚举实现时，消息键按约定自动生成
（`USER_NOT_FOUND` → `exception.user_not_found`），可被应用的 `messages.properties`、
数据库 Provider 或内置中英文 bundle 覆盖，全部未命中时回退枚举的 `desc`。

## 配置项

| 属性 | 默认值 | 说明 |
|---|---|---|
| `youngerier.trace.enabled` | `true` | 是否注册 traceId 过滤器（Servlet Web） |
| `youngerier.trace.header-name` | `X-Request-ID` | 上游传递 traceId 的请求头；取值须匹配 `[A-Za-z0-9._-]{1,128}` |
| `youngerier.trace.propagate-to-async` | `true` | 是否把 `MdcTaskDecorator` 注册为容器内唯一的 `TaskDecorator`，使 `@Async` 继承 traceId |
| `youngerier.exception-message.basenames` | 无 | 逗号分隔的 `ResourceBundle` 路径，作为异常消息来源之一 |
| `youngerier.rocketmq.enabled` | `true` | 是否装配链路 Hook 与 `RocketMqProducer` |

以上属性都带 `spring-configuration-metadata.json`，IDE 中可补全与校验。

## Excel 导出的安全约定

`ExcelTemplateRender` 的列取值表达式属于「模板」内容，可能来自数据库或配置中心。
实现使用只读数据绑定的 `SimpleEvaluationContext` 求值：支持属性读取与实例方法调用，
但**禁止 `T(...)` 类型引用、构造器与静态方法**，避免配置写入者获得任意代码执行能力。

`QueryWrapperHelper.withOrder(...)` 会把排序字段拼进 `ORDER BY` 的原始 SQL 片段，
因此 `QueryOrderField.getOrderField()` 必须返回可信的、编译期确定的列名（推荐实现为封闭枚举）。
库内另有标识符白名单作为兜底，非法字段抛 400 业务异常。

## 构建

```bash
mvn clean package            # 构建全部模块
mvn test                     # 运行全部测试
mvn clean deploy -P release  # 发布到 Maven Central（含 GPG 签名）
```

`codegen-core` 的 `GeneratedCodeIntegrationTest` 会跑一次完整的代码生成，并把生成产物
**真正编译一遍**（Lombok / MapStruct / MyBatis-Flex 处理器由 javac 自动发现），
因此它是「生成器 + toolkit 各模块」接缝的回归防线：一旦生成代码引用的 toolkit 类型被移动、
改名或签名变化，`mvn test` 就会失败。

## 设计约束

- **不产生 split package**：每个 Java 包只属于一个 artifact，可安全用于 JPMS 模块路径。
- **自动模块名**：各 `toolkit-*` jar 都声明了 `Automatic-Module-Name`，避免模块名退化为 jar 文件名。
- **可选依赖必须条件化**：引用可选依赖的自动装配一律使用嵌套配置类 + `@ConditionalOnClass(name = "...")`，
  因为方法级 `@ConditionalOnClass` 在注解元数据以反射方式读取（`@Import`、组件扫描、`register(Class)`）时
  会被静默跳过，把缺失的可选依赖变成启动期的 `NoClassDefFoundError`。
