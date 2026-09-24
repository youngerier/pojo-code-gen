package io.github.youngerier.generator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import io.github.youngerier.generator.analysis.ModelInput;
import io.github.youngerier.generator.analysis.PojoSourceScanner;
import io.github.youngerier.generator.analysis.TypeSolverFactory;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生成器端到端测试：扫描夹具实体完成一次完整生成，并把产物真正编译一遍。
 *
 * <p>注解处理器不显式指定，由 javac 从 classpath 自动发现 Lombok、MapStruct 与
 * MyBatis-Flex 处理器，与使用方项目的编译方式一致。
 */
class GeneratedCodeIntegrationTest {

    private static final String BASE_PACKAGE = "io.github.youngerier.generator.fixture";
    private static final String ENTITY_PACKAGE = BASE_PACKAGE + ".entity";

    /** 夹具实体源码：必须作为编译输入，MyBatis-Flex 处理器才会产出 TableDef */
    private static final List<String> FIXTURE_SOURCES = List.of(
            "io/github/youngerier/generator/fixture/entity/FixtureUser.java",
            "io/github/youngerier/generator/fixture/entity/FixtureUserTypeEnum.java");

    @TempDir
    Path outputBaseDir;

    @Test
    void generatesAllArtifactsAndTheyCompileAgainstToolkitModules() throws IOException {
        List<ModelInput> models = scanFixtureSources();
        assertEquals(1, models.size(), "应当只扫描到 FixtureUser 一个 @GenModel 实体");

        generate(models);

        List<Path> generated = collectGeneratedSources();
        assertEquals(10, generated.size(),
                "应当为每个实体生成 10 个产物，实际: " + relativeNames(generated));

        assertGeneratedContent();
        compileFixtureAndGeneratedCode(generated);
    }

    // ---------------- 扫描 ----------------

    /**
     * 与插件 Mojo 相同的方式：以测试源码目录为源根、当前测试 ClassLoader 为依赖加载器，
     * 直接扫描 .java 源文件，不依赖任何编译产物。
     */
    private List<ModelInput> scanFixtureSources() throws IOException {
        List<Path> sourceRoots = List.of(Path.of("src", "test", "java"));
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setSymbolResolver(new JavaSymbolSolver(TypeSolverFactory.combined(
                sourceRoots, Thread.currentThread().getContextClassLoader())));
        JavaParser javaParser = new JavaParser(configuration);
        return new PojoSourceScanner(List.of(ENTITY_PACKAGE), javaParser).scan(sourceRoots);
    }

    // ---------------- 生成 ----------------

    private void generate(List<ModelInput> models) throws IOException {
        Files.createDirectories(outputBaseDir);
        GeneratorConfig config = GeneratorConfig.builder()
                .outputBaseDir(outputBaseDir.toString())
                .models(models)
                .build();

        new GeneratorEngine(config).execute();
    }

    private List<Path> collectGeneratedSources() throws IOException {
        assertTrue(Files.isDirectory(outputBaseDir), "未生成输出目录: " + outputBaseDir);
        try (Stream<Path> walk = Files.walk(outputBaseDir)) {
            return walk.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static List<String> relativeNames(List<Path> paths) {
        return paths.stream().map(path -> path.getFileName().toString()).toList();
    }

    // ---------------- 内容断言 ----------------

    /**
     * 钉住生成代码与 toolkit 的契约：仅能编译不足以发现语义漂移。
     */
    private void assertGeneratedContent() throws IOException {
        String repository = readGenerated("dal", "repository", "FixtureUserRepository.java");
        assertTrue(repository.contains("import io.github.youngerier.support.page.flex.QueryWrapperHelper;"),
                "Repository 必须引用 toolkit-mybatis-flex 的 QueryWrapperHelper:\n" + repository);
        assertTrue(repository.contains("QueryWrapperHelper.withOrder(query)"),
                "Repository 必须通过 QueryWrapperHelper.withOrder 构造排序:\n" + repository);
        assertTrue(repository.contains("extends ServiceImpl<FixtureUserMapper, FixtureUser>"),
                "Repository 必须继承 MyBatis-Flex 的 ServiceImpl:\n" + repository);

        // 引用 APT 产出的 FixtureUserTableDef，静态实例与列均为小驼峰
        assertTrue(repository.contains(
                        "import io.github.youngerier.generator.fixture.entity.table.FixtureUserTableDef;"),
                "Repository 必须引用 FixtureUserTableDef:\n" + repository);
        assertTrue(repository.contains("FixtureUserTableDef.fixtureUser"),
                "TableDef 静态实例必须为小驼峰:\n" + repository);

        // 等值条件必须判空，不能把未传条件以 = null 拼进 SQL
        assertTrue(repository.contains("if (query.getUsername() != null)"),
                "Repository 必须逐字段判空:\n" + repository);
        assertTrue(repository.contains("fixtureUserTableDef.username.eq(query.getUsername())"),
                "TableDef 列引用必须为小驼峰:\n" + repository);
        assertTrue(repository.contains("fixtureUserTableDef.gmtCreate.ge(query.getMinGmtCreate())"),
                "时间范围列引用必须为小驼峰:\n" + repository);

        // 集合字段不是映射列：Repository 不得引用不存在的 TableDef 列
        assertFalse(repository.contains("tags"),
                "Repository 必须跳过 Collection 字段 tags:\n" + repository);

        String query = readGenerated("model", "request", "FixtureUserQuery.java");
        assertTrue(query.contains("import io.github.youngerier.support.enums.DefaultOrderField;"),
                "Query 必须引用 toolkit-core 的 DefaultOrderField:\n" + query);
        assertTrue(query.contains("import io.github.youngerier.support.page.AbstractPageQuery;"),
                "Query 必须继承 toolkit-core 的 AbstractPageQuery:\n" + query);
        assertTrue(query.contains("extends AbstractPageQuery<DefaultOrderField>"),
                "Query 必须继承 AbstractPageQuery<DefaultOrderField>:\n" + query);

        // 集合字段不能作为等值查询条件
        assertFalse(query.contains("tags"),
                "Query 必须跳过 Collection 字段 tags:\n" + query);

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

        // 注释中的 $ 经转义后，生成产物里必须仍可读且不导致生成崩溃
        String dto = readGenerated("model", "dto", "FixtureUserDTO.java");
        assertTrue(dto.contains("标价（美元，如 $5"),
                "DTO Javadoc 必须原样保留含 $ 的注释:\n" + dto);

        // 集合字段在 DTO/Request 中保留：它们是实体的真实属性，只是不是数据库列
        assertTrue(dto.contains("tags"), "DTO 必须保留集合字段 tags:\n" + dto);
        String request = readGenerated("model", "request", "FixtureUserRequest.java");
        assertTrue(request.contains("tags"), "Request 必须保留集合字段 tags:\n" + request);
    }

    private String readGenerated(String... pathSegments) throws IOException {
        Path file = outputBaseDir.resolve(BASE_PACKAGE.replace('.', '/'));
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

        // 模拟真实 Maven 布局：处理器要求沿 CLASS_OUTPUT 向上找到 pom.xml 才会读取配置
        Path projectDir = Files.createDirectories(outputBaseDir.resolve("project"));
        Path classOutput = Files.createDirectories(projectDir.resolve("target/classes"));
        Files.writeString(projectDir.resolve("pom.xml"), "");

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path aptOutputDir = Files.createDirectories(outputBaseDir.resolve("apt"));
        // 模拟插件：在 CLASS_OUTPUT 写入 APT 命名风格配置
        Files.writeString(classOutput.resolve("mybatis-flex.config"),
                "processor.tableDef.propertiesNameStyle = lowerCamelCase" + System.lineSeparator());
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(classOutput));
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
        assertTrue(Files.exists(classOutput.resolve(BASE_PACKAGE.replace('.', '/') + "/dal/repository/FixtureUserRepository.class")),
                "未产出 FixtureUserRepository.class");

        // APT 按配置生成小驼峰风格 TableDef
        Path aptTableDef = aptOutputDir.resolve(
                BASE_PACKAGE.replace('.', '/') + "/entity/table/FixtureUserTableDef.java");
        assertTrue(Files.exists(aptTableDef), "APT 未生成 FixtureUserTableDef");
        String tableDefSource = Files.readString(aptTableDef);
        assertTrue(tableDefSource.contains("public static final FixtureUserTableDef fixtureUser"),
                "TableDef 静态实例必须为小驼峰:\n" + tableDefSource);
        assertTrue(tableDefSource.contains("public final QueryColumn username"),
                "TableDef 列必须为小驼峰:\n" + tableDefSource);
        assertFalse(tableDefSource.contains("QueryColumn tags"),
                "TableDef 不得为集合字段生成列:\n" + tableDefSource);
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
