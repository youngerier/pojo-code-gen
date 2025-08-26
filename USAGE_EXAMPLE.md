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
                <version>1.0.0</version>
                <executions>
                    <execution>
                        <id>generate-code</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <scanPackages>
                        <package>com.example.entity</package>
                    </scanPackages>
                    <outputDir>target/generated-sources</outputDir>
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

### 2. 创建实体类

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

### 3. 运行代码生成

执行以下命令生成代码：

```bash
# 清理并重新编译，这会触发代码生成
mvn clean compile

# 或者单独执行代码生成插件
mvn io.github.youngerier:generator-maven-plugin:1.0.0:generate
```

### 4. 生成的代码结构

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

### 5. 使用生成的代码

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

### 插件配置参数

```xml
<configuration>
    <!-- 扫描的包名，支持多个 -->
    <scanPackages>
        <package>com.example.entity</package>
        <package>com.example.model</package>
    </scanPackages>
    
    <!-- 输出目录，默认为 target/generated-sources -->
    <outputDir>src/main/java</outputDir>
    
    <!-- 是否跳过代码生成，默认为 false -->
    <skip>false</skip>
</configuration>
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
   - 建议将 `target/generated-sources` 标记为源码目录

## 高级用法

### 自定义生成器

如果需要自定义代码生成逻辑，可以直接使用核心API：

```java
import io.github.youngerier.core.CodeGeneratorMain;
import io.github.youngerier.config.GeneratorConfig;

public class CustomGenerator {
    public static void main(String[] args) {
        GeneratorConfig config = new GeneratorConfig();
        config.setBasePackage("com.example");
        config.setOutputDir("src/main/java");
        config.setPojoClasses(Arrays.asList("com.example.entity.User"));
        
        CodeGeneratorMain.main(config);
    }
}
```

## 故障排除

1. **找不到 @GenModel 注解**：
   - 确保添加了正确的依赖
   - 检查包名是否正确

2. **代码生成失败**：
   - 检查实体类是否编译通过
   - 确保插件配置正确
   - 查看Maven日志输出

3. **生成的代码编译错误**：
   - 检查依赖版本是否匹配
   - 确保注解处理器配置正确