package io.github.youngerier.generator.maven;

import com.acme.entity.SampleEntity;
import com.acme.entity.sub.NestedSampleEntity;
import org.junit.jupiter.api.Test;
import zzz.other.OutsideEntity;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：{@code scanPackages} 必须是真正的过滤器。
 *
 * <p>缺陷背景：插件此前把包名交给 {@code ConfigurationBuilder.forPackages(...)}，
 * 但该配置在 Reflections 0.10.2 下不会限制 {@code getTypesAnnotatedWith} 的结果——
 * 即使把包名配成不存在的值，仍会返回 classpath 上任意包内的 {@code @GenModel} 类，
 * 导致插件扫描整个编译期 classpath（含依赖 jar），生成使用方没打算生成的实体。
 */
class PojoClassScannerTest {

    /** 当前模块的测试输出目录，夹具类都在这里 */
    private static final URL TEST_CLASSES = OutsideEntity.class.getProtectionDomain()
            .getCodeSource()
            .getLocation();

    private static Set<Class<?>> scan(String... packages) {
        return new PojoClassScanner(List.of(packages)).scan(List.of(TEST_CLASSES),
                PojoClassScannerTest.class.getClassLoader());
    }

    private static Set<String> names(Set<Class<?>> classes) {
        return classes.stream().map(Class::getName).collect(Collectors.toSet());
    }

    @Test
    void returnsClassesInConfiguredPackage() {
        Set<Class<?>> found = scan("com.acme.entity");

        assertTrue(found.contains(SampleEntity.class), "应包含配置包内的实体: " + names(found));
    }

    /**
     * 核心回归：配置包之外的 {@code @GenModel} 类绝不能被返回。
     */
    @Test
    void excludesClassesOutsideConfiguredPackage() {
        Set<Class<?>> found = scan("com.acme.entity");

        assertFalse(found.contains(OutsideEntity.class),
                "配置包外的实体不得被扫描到: " + names(found));
    }

    /**
     * 核心回归：配置一个不存在的包时必须返回空集合，而不是把整个 classpath 扫出来。
     */
    @Test
    void returnsNothingForUnknownPackage() {
        assertEquals(Set.of(), scan("zzz.nothing"));
    }

    @Test
    void includesSubPackages() {
        Set<Class<?>> found = scan("com.acme.entity");

        assertTrue(found.contains(NestedSampleEntity.class), "应覆盖子包: " + names(found));
    }

    @Test
    void doesNotTreatPackagePrefixAsMatch() {
        // com.acme.en 不是 com.acme.entity 的父包边界，不能命中
        assertEquals(Set.of(), scan("com.acme.en"));
        // com.acme 是真正的父包，可以命中
        assertTrue(scan("com.acme").contains(SampleEntity.class));
    }

    @Test
    void supportsMultiplePackages() {
        Set<Class<?>> found = scan("com.acme.entity", "zzz.other");

        assertTrue(found.contains(SampleEntity.class));
        assertTrue(found.contains(OutsideEntity.class));
    }

    @Test
    void emptyOrBlankPackagesReturnNothing() {
        assertEquals(Set.of(), scan());
        assertEquals(Set.of(), scan("   "));
        assertEquals(Set.of(), new PojoClassScanner(null).scan(List.of(TEST_CLASSES),
                getClass().getClassLoader()));
    }

    @Test
    void normalizesTrailingDotAndWhitespace() {
        // 用 Arrays.asList 以便显式包含 null（List.of 不允许 null 元素）
        PojoClassScanner scanner = new PojoClassScanner(Arrays.asList("  com.acme.entity.  ", "", null));

        assertEquals(Set.of("com.acme.entity"), scanner.configuredPackages());
        assertTrue(scanner.isInConfiguredPackage(SampleEntity.class));
        assertTrue(scanner.isInConfiguredPackage(NestedSampleEntity.class));
        assertFalse(scanner.isInConfiguredPackage(OutsideEntity.class));
    }

    @Test
    void resultsAreSortedForReproducibleGenerationOrder() {
        List<Class<?>> ordered = List.copyOf(scan("com.acme.entity"));

        assertEquals(ordered.stream().map(Class::getName).sorted().toList(),
                ordered.stream().map(Class::getName).toList());
    }
}
