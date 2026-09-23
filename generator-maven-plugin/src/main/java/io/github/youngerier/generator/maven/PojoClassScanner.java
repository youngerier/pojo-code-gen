package io.github.youngerier.generator.maven;

import io.github.youngerier.generator.annotation.GenModel;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 在指定包内扫描标注了 {@link GenModel} 的 POJO 类。
 *
 * <p><b>为什么需要显式包过滤</b>：此前直接把 {@code scanPackages} 交给
 * {@code ConfigurationBuilder.forPackages(...)}，但该配置在 Reflections 0.10.2 下
 * <em>不会</em>限制 {@code getTypesAnnotatedWith} 的结果——实测把包名配成不存在的
 * {@code zzz.nothing}，仍会返回 classpath 上任意包内的 {@code @GenModel} 类，
 * 于是插件会扫描整个编译期 classpath（包括依赖 jar），把使用方没打算生成的实体一并生成。
 *
 * <p>因此这里以「显式包名前缀白名单」作为权威过滤条件，{@code forPackages} 仅作为
 * 对可能遵循该配置的 Reflections 版本的优化提示保留。
 */
public final class PojoClassScanner {

    /**
     * 被视为「包内」的包名前缀，已去除空白与结尾的点
     */
    private final Set<String> packages;

    /**
     * @param packages 期望扫描的包名，允许为 null / 空集合（此时不返回任何类）
     */
    public PojoClassScanner(Collection<String> packages) {
        this.packages = packages == null ? Set.of() : packages.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(name -> name.endsWith(".") ? name.substring(0, name.length() - 1) : name)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 扫描并返回包内标注了 {@link GenModel} 的类，结果按类名排序以保证生成顺序可复现。
     *
     * @param urls        待扫描的 classpath 元素（编译输出目录、依赖 jar 等）
     * @param classLoader 用于加载候选类的类加载器
     * @return 匹配的类；未配置任何包时返回空集合
     */
    public Set<Class<?>> scan(Collection<URL> urls, ClassLoader classLoader) {
        if (packages.isEmpty() || urls == null || urls.isEmpty()) {
            return Set.of();
        }

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(List.copyOf(urls))
                .setScanners(Scanners.TypesAnnotated)
                .forPackages(packages.toArray(String[]::new))
                .addClassLoaders(classLoader));

        return reflections.getTypesAnnotatedWith(GenModel.class).stream()
                // 权威过滤：不依赖 Reflections 是否真的应用了 forPackages
                .filter(this::isInConfiguredPackage)
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Class::getName))));
    }

    /**
     * @return 类所在包是否等于、或位于任一配置包之下（包含子包）
     */
    public boolean isInConfiguredPackage(Class<?> type) {
        Package typePackage = type.getPackage();
        String packageName = typePackage == null ? "" : typePackage.getName();
        return packages.stream()
                .anyMatch(configured -> packageName.equals(configured)
                        || packageName.startsWith(configured + "."));
    }

    /**
     * @return 归一化后的配置包名（供日志/测试使用）
     */
    public Set<String> configuredPackages() {
        return Set.copyOf(packages);
    }
}
