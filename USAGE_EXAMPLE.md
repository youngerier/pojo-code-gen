# Java Code Generation Toolkit 使用指南

## 快速开始

### 1. 添加Maven依赖

在你的项目 `pom.xml` 中添加以下配置：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.24</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- MyBatis Flex -->
        <dependency>
            <groupId>com.mybatis-flex</groupId>
            <artifactId>mybatis-flex-core</artifactId>
            <version>1.10.0</version>
        </dependency>
        
        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>1.5.5.Final</version>
        </dependency>
        
        <!-- Spring Web (如果需要Controller层) -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
            <version>6.2.7</version>
        </dependency>
        
        <!-- Validation API -->
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
            <version>3.0.2</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Java Code Generation Plugin -->
            <plugin>
                <groupId>io.github.youngerier</groupId>
                <artifactId>generator-maven-plugin</artifactId>
                <version>1.0.1</version>
                <executions>
                    <execution>
                        <id>generate-code</id>
                        <phase>process-classes</phase>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <scanPackages>
                        <package>com.example.entity</package>
                    </scanPackages>
                    <outputDir>${project.build.directory}/generated-sources</outputDir>
                </configuration>
            </plugin>

            <!-- Maven Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.24</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>1.5.5.Final</version>
                        </path>
                        <path>
                            <groupId>com.mybatis-flex</groupId>
                            <artifactId>mybatis-flex-processor</artifactId>
                            <version>1.10.0</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2. 添加插件依赖

除了核心的代码生成插件，你还需要添加相关的依赖库：

```xml
<dependencies>
    <!-- 代码生成核心库（如果需要自定义生成器）-->
    <dependency>
        <groupId>io.github.youngerier</groupId>
        <artifactId>codegen-core</artifactId>
        <version>1.0.1</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- 工具包（包含通用类和工具）-->
    <dependency>
        <groupId>io.github.youngerier</groupId>
        <artifactId>toolkit</artifactId>
        <version>1.0.1</version>
    </dependency>
</dependencies>
```

### 3. 创建实体类

在 `src/main/java/com/example/entity` 目录下创建实体类：

```java
package com.example.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import io.github.youngerier.annotation.GenModel;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@GenModel
@Table("user")
public class User {
    
    @Id
    private Long id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Size(max = 11, message = "手机号长度不能超过11位")
    private String phone;
    
    private String nickname;
    
    private Integer age;
    
    private String avatar;
}
```

```java
package com.example.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import io.github.youngerier.annotation.GenModel;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@GenModel
@Table("product")
public class Product {
    
    @Id
    private Long id;
    
    @NotBlank(message = "产品名称不能为空")
    private String name;
    
    private String description;
    
    @NotNull(message = "价格不能为空")
    private Double price;
    
    private Integer stock;
    
    private String category;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
```

### 4. 运行代码生成

执行以下命令生成代码：

```bash
# 清理并重新编译，这会触发代码生成
mvn clean compile

# 或者单独执行代码生成插件
mvn pojo-codegen:generate

# 使用完整的插件坐标
mvn io.github.youngerier:generator-maven-plugin:1.0.1:generate
```

### 5. 生成的代码结构

执行后会在 `target/generated-sources` 目录下生成以下文件：

```
target/generated-sources/
├── dto/
│   ├── UserDto.java
│   └── ProductDto.java
├── request/
│   ├── UserRequest.java
│   └── ProductRequest.java
├── response/
│   ├── UserResponse.java
│   └── ProductResponse.java
├── query/
│   ├── UserQuery.java
│   └── ProductQuery.java
├── service/
│   ├── UserService.java
│   └── ProductService.java
├── service/impl/
│   ├── UserServiceImpl.java
│   └── ProductServiceImpl.java
├── repository/
│   ├── UserRepository.java
│   └── ProductRepository.java
└── converter/
    ├── UserConverter.java
    └── ProductConverter.java
```

### 6. 使用生成的代码

#### 在Controller中使用：

```java
package com.example.controller;

import com.example.request.UserRequest;
import com.example.response.UserResponse;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public UserResponse createUser(@RequestBody UserRequest request) {
        return userService.create(request);
    }
    
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getById(id);
    }
    
    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        return userService.updateById(id, request);
    }
    
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
    }
}
```

#### 在Service中使用转换器：

```java
package com.example.service.impl;

import com.example.converter.UserConverter;
import com.example.entity.User;
import com.example.repository.UserRepository;
import com.example.request.UserRequest;
import com.example.response.UserResponse;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomUserServiceImpl {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserConverter userConverter;
    
    public UserResponse createUser(UserRequest request) {
        User user = userConverter.requestToEntity(request);
        User savedUser = userRepository.save(user);
        return userConverter.entityToResponse(savedUser);
    }
}
```

## 配置选项

### Maven插件完整配置

```
<plugin>
    <groupId>io.github.youngerier</groupId>
    <artifactId>generator-maven-plugin</artifactId>
    <version>1.0.1</version>
    <executions>
        <execution>
            <id>generate-code</id>
            <!-- 推荐绑定到 process-classes 阶段，确保类文件已编译 -->
            <phase>process-classes</phase>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <!-- 扫描的包名，支持多个包 -->
        <scanPackages>
            <package>com.example.entity</package>
            <package>com.example.model</package>
            <package>com.abc.domain</package>
        </scanPackages>
        
        <!-- 输出目录，默认为 ${project.build.directory}/generated-sources/ -->
        <!-- 最终代码会生成在 outputDir/src/main/java 目录下 -->
        <outputDir>${project.build.directory}/generated-sources</outputDir>
    </configuration>
</plugin>
```

### 插件配置参数详解

| 参数名 | 类型 | 默认值 | 描述 |
|--------|------|--------|------|
| `scanPackages` | `List<String>` | 无 | **必需参数**。要扫描的包名列表，插件会在这些包中查找带有 `@GenModel` 注解的类 |
| `outputDir` | `File` | `${project.build.directory}/generated-sources/` | 代码生成的基础目录，最终代码位于此目录下的 `src/main/java` 文件夹中 |

### 支持的Maven命令

```bash
# 1. 通过生命周期触发（推荐）
mvn clean compile

# 2. 直接执行插件目标
mvn pojo-codegen:generate

# 3. 完整的插件目标执行
mvn io.github.youngerier:generator-maven-plugin:1.0.1:generate

# 4. 带参数执行
mvn pojo-codegen:generate -Dpojo.codegen.scanPackages=com.example.entity,com.example.model
```

### 插件属性配置

除了在 `pom.xml` 中配置，也可以通过系统属性传递参数：

```bash
# 通过命令行指定扫描包
mvn pojo-codegen:generate -Dpojo.codegen.scanPackages=com.example.entity

# 通过命令行指定输出目录
mvn pojo-codegen:generate -Dpojo.codegen.outputDir=src/main/java
```

### 多模块项目配置

在多模块项目中，建议在需要代码生成的子模块中单独配置插件：

```xml
<!-- 在子模块的 pom.xml 中 -->
<build>
    <plugins>
        <plugin>
            <groupId>io.github.youngerier</groupId>
            <artifactId>generator-maven-plugin</artifactId>
            <version>1.0.1</version>
            <executions>
                <execution>
                    <id>generate-code</id>
                    <phase>process-classes</phase>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <scanPackages>
                    <package>com.example.module1.entity</package>
                </scanPackages>
                <outputDir>${project.build.directory}/generated-sources</outputDir>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## 注意事项

1. **实体类要求**：
   - 必须添加 `@GenModel` 注解
   - 建议使用 Lombok 的 `@Data` 注解
   - 支持 MyBatis Flex 和 JPA 注解

2. **依赖版本**：
   - Java 17+
   - Lombok 1.18.24+
   - MyBatis Flex 1.10.0+
   - MapStruct 1.5.5.Final+

3. **IDE支持**：
   - 生成的代码会自动加入到编译路径
   - IDE可以正常识别和使用生成的类
   - 建议将 `target/generated-sources/src/main/java` 标记为源码目录
   - IntelliJ IDEA 通常会自动识别，Eclipse 可能需要手动刷新项目

4. **插件执行阶段**：
   - 默认绑定到 `process-classes` 阶段，确保在代码生成前类文件已编译
   - 支持直接执行插件目标 `mvn pojo-codegen:generate`
   - 如果类文件不存在，插件会自动尝试编译项目

## 高级用法

### 自定义生成器

如果需要自定义代码生成逻辑，可以直接使用核心API：

```
import io.github.youngerier.generator.GeneratorConfig;
import io.github.youngerier.generator.GeneratorEngine;
import java.io.File;
import java.util.Collections;

public class CustomGenerator {
    public static void main(String[] args) {
        try {
            // 创建POJO类对象
            Class<?> pojoClass = Class.forName("com.example.entity.User");
            
            // 构建 GeneratorConfig
            GeneratorConfig config = GeneratorConfig.builder()
                    .moduleName("example")
                    .outputBaseDir("target" + File.separator + "generated-sources")
                    .pojoClasses(Collections.singletonList(pojoClass))
                    .build();

            // 创建并执行引擎
            GeneratorEngine engine = new GeneratorEngine(config);
            engine.execute();
        } catch (ClassNotFoundException e) {
            System.err.println("找不到指定的POJO类: com.example.entity.User");
            e.printStackTrace();
        }
    }
}
```

## 故障排除

### 常见问题及解决方案

1. **找不到 @GenModel 注解**：
   - 确保添加了正确的依赖 `codegen-core`
   - 检查包名是否正确
   - 确认实体类已编译成功

2. **插件找不到带有 @GenModel 注解的类**：
   ```bash
   # 确保项目已编译
   mvn clean compile
   
   # 检查 target/classes 目录下是否有类文件
   ls -la target/classes/com/example/entity/
   
   # 重新执行代码生成
   mvn pojo-codegen:generate
   ```

3. **插件执行失败**：
   ```xml
   <!-- 确保插件配置正确 -->
   <plugin>
       <groupId>io.github.youngerier</groupId>
       <artifactId>generator-maven-plugin</artifactId>
       <version>1.0.1</version>
       <!-- 检查版本号是否正确 -->
   </plugin>
   ```

4. **生成的代码编译错误**：
   - 检查依赖版本是否匹配
   - 确保注解处理器配置正确
   - 检查 Java 版本兼容性（需要 Java 17+）

5. **IDE 不识别生成的代码**：
   ```bash
   # 刷新 Maven 项目
   mvn clean compile
   
   # 在 IDE 中刷新项目
   # IntelliJ IDEA: Ctrl+Shift+F9 或 Build -> Rebuild Project
   # Eclipse: F5 或 Project -> Refresh
   ```

6. **多模块项目中的问题**：
   - 确保在正确的子模块中配置插件
   - 检查模块间的依赖关系
   - 遵循先编译再生成的序列

### 调试技巧

```bash
# 开启 Maven 详细日志
mvn pojo-codegen:generate -X

# 检查插件信息
mvn help:describe -Dplugin=io.github.youngerier:generator-maven-plugin:1.0.1

# 检查项目的类路径
mvn dependency:build-classpath
```