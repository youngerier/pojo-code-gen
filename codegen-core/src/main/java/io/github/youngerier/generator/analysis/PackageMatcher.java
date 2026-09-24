package io.github.youngerier.generator.analysis;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 扫描包白名单匹配器：判断一个包是否<strong>等于、或位于任一配置包之下</strong>（包含子包）。
 *
 * <p>边界按包段判定，{@code com.acme.en} 不会被误判为 {@code com.acme.entity} 的前缀。
 */
public final class PackageMatcher {

    private final Set<String> packages;

    public PackageMatcher(Collection<String> packages) {
        this.packages = packages == null ? Set.of() : packages.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(name -> name.endsWith(".") ? name.substring(0, name.length() - 1) : name)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * @return 实际包名等于或位于任一配置包之下
     */
    public boolean matches(String packageName) {
        String actual = packageName == null ? "" : packageName;
        return packages.stream()
                .anyMatch(configured -> actual.equals(configured)
                        || actual.startsWith(configured + "."));
    }

    public boolean isEmpty() {
        return packages.isEmpty();
    }

    /**
     * @return 归一化后的配置包名（供日志/测试使用）
     */
    public Set<String> configuredPackages() {
        return Set.copyOf(packages);
    }
}
