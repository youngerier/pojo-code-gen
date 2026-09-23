package io.github.youngerier.generator;

import io.github.youngerier.generator.fixture.entity.FixtureUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生成器端到端集成测试：对夹具实体跑一次完整生成，然后把生成产物<strong>真正编译一遍</strong>。
 *
 * <p>这是本仓库里唯一覆盖「生成器 + toolkit 各模块」接缝的地方，替代了原先只能靠人工
 * 执行 {@code example} 模块来完成的验证。它能在编译期发现：
 *
 * <ul>
 *     <li>生成代码引用的 toolkit 类型被移动或改名（例如
 *     {@code QueryWrapperHelper} 从 {@code support.page} 移到 {@code support.page.flex}）；</li>
 *     <li>生成代码引用的方法签名变化（例如 {@code Pagination.of}、{@code BaseException.badRequest}）；</li>
 *     <li>生成器自身产出的代码语法/类型错误。</li>
 * </ul>
 *
 * <p>注解处理不显式指定处理器，交由 javac 从 classpath 自动发现 Lombok、MapStruct 与
 * MyBatis-Flex 的处理器——生成的 {@code @Data} 访问器、{@code UserConvertor} 实现和
 * {@code TableRefs} 都依赖它们，这与使用方项目的实际编译方式一致。
 */
class GeneratedCodeIntegrationTest {

    private static final String BASE_PACKAGE = "io.github.youngerier.generator.fixture";

    /** 夹具实体源码：必须作为编译输入，MyBatis-Flex 处理器才会产出 TableRefs */
    private static final List<String> FIXTURE_SOURCES = List.of(
            "io/github/youngerier/generator/fixture/entity/FixtureUser.java",
            "io/github/youngerier/generator/fixture/entity/FixtureUserTypeEnum.java");

    @TempDir
    Path outputBaseDir;

    @TempDir
    Path classesDir;

    @Test
    void generatesAllArtifactsAndTheyCompileAgainstToolkitModules() throws IOException {
        generate();

        List<Path> generated = collectGeneratedSources();
        assertEquals(10, generated.size(),
                "应当为每个实体生成 10 个产物，实际: " + relativeNames(generated));

        assertGeneratedContent();
        compileFixtureAndGeneratedCode(generated);
    }

    // ---------------- 生成 ----------------

    private void generate() {
        GeneratorConfig config = GeneratorConfig.builder()
                .moduleName("codegen-core-it")
                .outputBaseDir(outputBaseDir.toString())
                .pojoClasses(List.of(FixtureUser.class))
                .build();

        new GeneratorEngine(config).execute();
    }

    private List<Path> collectGeneratedSources() throws IOException {
        Path sourceRoot = outputBaseDir.resolve("src").resolve("main").resolve("java");
        assertTrue(Files.isDirectory(sourceRoot), "未生成源码目录: " + sourceRoot);
        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            return walk.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static List<String> relativeNames(List<Path> paths) {
        return paths.stream().map(path -> path.getFileName().toString()).toList();
    }

    // ---------------- 内容断言 ----------------

    /**
     * 逐条钉住生成代码与 toolkit 的契约。相比「能编译」，这些断言还能发现
     * 「编译得过去但语义已漂移」的情况（例如 Repository 不再走 ORDER BY 适配层）。
     */
    private void assertGeneratedContent() throws IOException {
        String repository = readGenerated("dal", "repository", "FixtureUserRepository.java");
        assertTrue(repository.contains("import io.github.youngerier.support.page.flex.QueryWrapperHelper;"),
                "Repository 必须引用 toolkit-mybatis-flex 的 QueryWrapperHelper:\n" + repository);
        assertTrue(repository.contains("QueryWrapperHelper.withOrder(query)"),
                "Repository 必须通过 QueryWrapperHelper.withOrder 构造排序:\n" + repository);
        assertTrue(repository.contains("extends ServiceImpl<FixtureUserMapper, FixtureUser>"),
                "Repository 必须继承 MyBatis-Flex 的 ServiceImpl:\n" + repository);

        String query = readGenerated("model", "request", "FixtureUserQuery.java");
        assertTrue(query.contains("import io.github.youngerier.support.enums.DefaultOrderField;"),
                "Query 必须引用 toolkit-core 的 DefaultOrderField:\n" + query);
        assertTrue(query.contains("import io.github.youngerier.support.page.AbstractPageQuery;"),
                "Query 必须继承 toolkit-core 的 AbstractPageQuery:\n" + query);
        assertTrue(query.contains("extends AbstractPageQuery<DefaultOrderField>"),
                "Query 必须继承 AbstractPageQuery<DefaultOrderField>:\n" + query);

        String serviceImpl = readGenerated("service", "impl", "FixtureUserServiceImpl.java");
        assertTrue(serviceImpl.contains("import io.github.youngerier.support.page.Pagination;"),
                "ServiceImpl 必须引用 toolkit-core 的 Pagination:\n" + serviceImpl);
        assertTrue(serviceImpl.contains("Pagination.of(page.getRecords(), query, page.getTotalRow())"),
                "ServiceImpl 必须通过 Pagination.of 组装分页结果:\n" + serviceImpl);

        String controller = readGenerated("controller", "FixtureUserController.java");
        assertTrue(controller.contains("import io.github.youngerier.support.Response;"),
                "Controller 必须引用 toolkit-core 的 Response:\n" + controller);
        assertTrue(controller.contains("Response<Pagination<FixtureUserDTO>>"),
                "Controller 分页接口必须返回 Response<Pagination<...>>:\n" + controller);

        // 自定义枚举字段必须被正确解析到实体所在包
        assertTrue(query.contains("FixtureUserTypeEnum"),
                "Query 必须包含枚举字段类型:\n" + query);
        assertTrue(query.contains("import " + BASE_PACKAGE + ".entity.FixtureUserTypeEnum;"),
                "Query 必须导入实体包下的枚举:\n" + query);
    }

    private String readGenerated(String... pathSegments) throws IOException {
        Path file = outputBaseDir.resolve("src").resolve("main").resolve("java")
                .resolve(BASE_PACKAGE.replace('.', '/'));
        for (String segment : pathSegments) {
            file = file.resolve(segment);
        }
        assertTrue(Files.exists(file), "缺少生成产物: " + file);
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    // ---------------- 编译 ----------------

    private void compileFixtureAndGeneratedCode(List<Path> generated) throws IOException {
        Path fixtureSourceRoot = Path.of("src", "test", "java");
        List<Path> sources = new ArrayList<>(generated);
        for (String relative : FIXTURE_SOURCES) {
            Path source = fixtureSourceRoot.resolve(relative);
            assertTrue(Files.exists(source), "缺少夹具源码: " + source.toAbsolutePath());
            sources.add(source);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "当前 JDK 未提供 javac，无法执行编译断言");

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        // javac 要求 SOURCE_OUTPUT 目录已存在
        Path aptOutputDir = Files.createDirectories(outputBaseDir.resolve("apt"));
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(classesDir));
            fileManager.setLocationFromPaths(StandardLocation.SOURCE_OUTPUT, List.of(aptOutputDir));

            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(sources);
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-proc:full",
                    "-nowarn");
            Boolean success = compiler.getTask(null, fileManager, diagnostics, options, null, units).call();

            List<Diagnostic<? extends JavaFileObject>> errors = diagnostics.getDiagnostics().stream()
                    .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                    .toList();
            assertEquals(List.of(), errors, () -> "生成的代码无法编译:\n" + format(errors));
            assertEquals(Boolean.TRUE, success, () -> "编译失败:\n" + format(errors));
        }

        // 生成产物确实被编译成了 class 文件
        assertTrue(Files.exists(classesDir.resolve(BASE_PACKAGE.replace('.', '/') + "/dal/repository/FixtureUserRepository.class")),
                "未产出 FixtureUserRepository.class");
    }

    private static String format(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        return diagnostics.stream()
                .map(diagnostic -> "%s:%d: %s".formatted(
                        diagnostic.getSource() == null ? "<unknown>" : diagnostic.getSource().getName(),
                        diagnostic.getLineNumber(),
                        diagnostic.getMessage(Locale.ROOT)))
                .collect(Collectors.joining("\n"));
    }
}
