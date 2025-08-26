# Maven Central 发布指南

本指南将帮助你将 POJO Code Generator 项目发布到 Maven Central。

## 前置条件

### 1. 注册 Sonatype OSSRH 账户

1. 访问 [Sonatype JIRA](https://issues.sonatype.org/secure/Signup!default.jspa) 注册账户
2. 创建一个新的 Issue 来申请 `io.github.youngerier` 命名空间
   - Project: Community Support - Open Source Project Repository Hosting (OSSRH)
   - Issue Type: New Project
   - Summary: Request for io.github.youngerier
   - Group Id: io.github.youngerier
   - Project URL: https://github.com/youngerier/pojo-code-gen
   - SCM URL: https://github.com/youngerier/pojo-code-gen.git

### 2. 生成 GPG 密钥

```bash
# 生成 GPG 密钥对
gpg --gen-key

# 列出密钥
gpg --list-keys

# 上传公钥到密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

### 3. 配置 Maven Settings

将 `settings.xml.template` 复制到 `~/.m2/settings.xml` 并填入你的凭据：

```bash
cp settings.xml.template ~/.m2/settings.xml
```

编辑 `~/.m2/settings.xml`，替换以下占位符：
- `YOUR_SONATYPE_USERNAME`: 你的 Sonatype JIRA 用户名
- `YOUR_SONATYPE_PASSWORD`: 你的 Sonatype JIRA 密码
- `YOUR_GPG_PASSPHRASE`: 你的 GPG 密钥密码

## 发布步骤

### 1. 更新版本号

确保所有模块的版本号都是发布版本（不包含 SNAPSHOT）：

```bash
# 使用 Maven versions plugin 更新版本
mvn versions:set -DnewVersion=1.0.0
mvn versions:commit
```

### 2. 构建和测试

```bash
# 清理并编译项目
mvn clean compile

# 运行测试（如果有）
mvn test
```

### 3. 发布到 Maven Central

```bash
# 使用 release profile 进行发布
mvn clean deploy -P release

# 或者分步骤执行
mvn clean deploy -P release -s ./settings.xml -X
```

### 4. 在 Sonatype Nexus 中发布

1. 登录 [Sonatype Nexus Repository Manager](https://s01.oss.sonatype.org/)
2. 点击左侧菜单的 "Staging Repositories"
3. 找到你的 staging repository（通常以 `iogithub-` 开头）
4. 选择该 repository，点击 "Close" 按钮
5. 等待验证完成后，点击 "Release" 按钮

### 5. 验证发布

发布完成后，通常需要等待几分钟到几小时，你的包就会出现在：
- [Maven Central Search](https://search.maven.org/)
- [MVN Repository](https://mvnrepository.com/)

## 使用发布的包

其他项目可以通过以下方式使用你的包：

```xml
<dependency>
    <groupId>io.github.youngerier</groupId>
    <artifactId>generator-core</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>io.github.youngerier</groupId>
    <artifactId>toolkit</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 故障排除

### 常见问题

1. **GPG 签名失败**
   - 确保 GPG 密钥已正确生成并上传到密钥服务器
   - 检查 `settings.xml` 中的 GPG 配置

2. **Sonatype 验证失败**
   - 确保所有必需的文件都已包含（sources, javadoc）
   - 检查 POM 文件中的元数据是否完整

3. **权限问题**
   - 确保你的 Sonatype JIRA 账户已获得 `io.github.youngerier` 命名空间的权限

### 有用的命令

```bash
# 检查 GPG 密钥
gpg --list-secret-keys

# 验证签名
gpg --verify target/generator-core-1.0.0.jar.asc

# 查看 staging repositories
mvn nexus-staging:rc-list

# 手动关闭 staging repository
mvn nexus-staging:close

# 手动发布 staging repository
mvn nexus-staging:release
```

## 自动化发布

你也可以考虑使用 GitHub Actions 来自动化发布过程。创建 `.github/workflows/release.yml` 文件来配置自动发布流程。

## 注意事项

1. 确保项目的 README.md、LICENSE 等文件都是最新的
2. 版本号应该遵循语义化版本规范
3. 发布前请确保代码已经过充分测试
4. 一旦发布到 Maven Central，版本就不能被删除或修改