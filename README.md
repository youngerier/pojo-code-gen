# POJO Code Generator

POJO Code Generator 是一个基于 Java 的代码生成工具，旨在简化和加速后端开发过程中常见的数据传输对象（DTO）、请求对象（Request）、响应对象（Response）、查询对象（Query）、服务接口（Service）、服务实现（ServiceImpl）、数据仓库接口（Repository）以及 MapStruct 转换器等样板代码的生成。

## 功能

*   **DTO 生成**: 根据实体类生成对应的数据传输对象。
*   **Request 生成**: 生成用于接收前端请求的参数对象。
*   **Response 生成**: 生成用于向前端返回数据的响应对象。
*   **Query 生成**: 生成用于数据库查询的条件对象。
*   **Service 接口生成**: 生成业务逻辑层的服务接口。
*   **ServiceImpl 实现生成**: 生成服务接口的实现类，包含基本的 CRUD 操作。
*   **Repository 接口生成**: 生成基于 MyBatis Flex 的数据访问层接口。
*   **MapStruct Convertor 生成**: 生成实体与 DTO/Request/Response 之间转换的 MapStruct 映射接口。

## 技术栈

*   **Java**: 核心开发语言。
*   **Maven**: 项目管理和构建工具。
*   **Lombok**: 简化 Java Bean 开发，减少样板代码。
*   **MyBatis Flex**: 强大的 ORM 框架，用于 Repository 层。
*   **JavaPoet**: 用于生成 `.java` 源文件的库。
*   **MapStruct**: 编译时代码生成器，用于简化 Java Bean 之间的映射。

## 如何构建

1.  **克隆仓库**:
    ```bash
    git clone https://github.com/youngerier/pojo-code-gen.git
    cd pojo-code-gen
    ```
2.  **使用 Maven 构建**:
    ```bash
    mvn clean install
    ```
    这将编译项目并将生成的 JAR 包安装到本地 Maven 仓库。

## 如何使用

项目的入口点是 `com.abc.CodeGeneratorMain` 类。您可以通过修改此类的 `main` 方法来配置代码生成。

### 配置 `PackageConfig`

`PackageConfig` 类用于定义生成代码的包结构。

```java
// CodeGeneratorMain.java 示例
PackageConfig packageLayout = new PackageConfig("com.yourcompany.yourproject");
```

### 配置 `PojoInfo`

`PojoInfo` 类用于描述要生成代码的 POJO（Plain Old Java Object）信息，包括类名、包名、类注释和字段信息。

```java
// CodeGeneratorMain.java 示例
PojoInfo userPojoInfo = new PojoInfo();
userPojoInfo.setClassName("User");
userPojoInfo.setPackageName("com.yourcompany.yourproject.entity");
userPojoInfo.setClassComment("用户实体");

// 添加字段
userPojoInfo.addField(new PojoInfo.FieldInfo("id", "java.lang.Long", "用户ID", true));
userPojoInfo.addField(new PojoInfo.FieldInfo("username", "java.lang.String", "用户名"));
userPojoInfo.addField(new PojoInfo.FieldInfo("email", "java.lang.String", "邮箱"));
// ... 添加更多字段
```

### 运行代码生成器

在 `CodeGeneratorMain.java` 中，您可以实例化不同的 `CodeGenerator` 实现，并调用它们的 `generate` 方法来生成代码。

```java
// CodeGeneratorMain.java 示例
import com.example.generator.CodeGenerator;
import com.example.generator.FileGenerator;
import com.example.generator.model.PackageLayout;
import com.example.generator.model.PojoInfo;
import com.example.generator.generators.*;

public class CodeGeneratorMain {
    public static void main(String[] args) {
        PackageConfig packageLayout = new PackageConfig("com.yourcompany.yourproject");

        PojoInfo userPojoInfo = new PojoInfo();
        userPojoInfo.setClassName("User");
        userPojoInfo.setPackageName("com.yourcompany.yourproject.entity");
        userPojoInfo.setClassComment("用户实体");
        userPojoInfo.addField(new PojoInfo.FieldInfo("id", "java.lang.Long", "用户ID", true));
        userPojoInfo.addField(new PojoInfo.FieldInfo("username", "java.lang.String", "用户名"));
        userPojoInfo.addField(new PojoInfo.FieldInfo("email", "java.lang.String", "邮箱"));

        FileGenerator fileGenerator = new FileGenerator("/path/to/your/output/directory"); // 指定输出目录

        // 生成 DTO
        CodeGenerator dtoGenerator = new DtoGenerator(packageLayout);
        fileGenerator.generateFile(dtoGenerator, userPojoInfo);

        // 生成 Request
        CodeGenerator requestGenerator = new RequestGenerator(packageLayout);
        fileGenerator.generateFile(requestGenerator, userPojoInfo);

        // 生成 Response
        CodeGenerator responseGenerator = new ResponseGenerator(packageLayout);
        fileGenerator.generateFile(responseGenerator, userPojoInfo);

        // 生成 Query
        CodeGenerator queryGenerator = new QueryGenerator(packageLayout);
        fileGenerator.generateFile(queryGenerator, userPojoInfo);

        // 生成 Service
        CodeGenerator serviceGenerator = new ServiceGenerator(packageLayout);
        fileGenerator.generateFile(serviceGenerator, userPojoInfo);

        // 生成 ServiceImpl
        CodeGenerator serviceImplGenerator = new ServiceImplGenerator(packageLayout);
        fileGenerator.generateFile(serviceImplGenerator, userPojoInfo);

        // 生成 Repository
        CodeGenerator repositoryGenerator = new RepositoryGenerator(packageLayout);
        fileGenerator.generateFile(repositoryGenerator, userPojoInfo);

        // 生成 MapStruct Convertor
        CodeGenerator mapstructGenerator = new MapstructGenerator(packageLayout);
        fileGenerator.generateFile(mapstructGenerator, userPojoInfo);

        System.out.println("代码生成完成！");
    }
}
```

请将 `/path/to/your/output/directory` 替换为您希望生成代码的实际输出目录。