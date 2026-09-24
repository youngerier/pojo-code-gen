package io.github.youngerier.generator.analysis;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PackageMatcher} 包白名单边界回归测试。
 */
class PackageMatcherTest {

    @Test
    void matchesConfiguredPackageAndSubPackages() {
        PackageMatcher matcher = new PackageMatcher(List.of("com.acme.entity"));

        assertTrue(matcher.matches("com.acme.entity"));
        assertTrue(matcher.matches("com.acme.entity.sub"));
        assertFalse(matcher.matches("zzz.other"));
    }

    /**
     * 包边界按包段判定：com.acme.en 不是 com.acme.entity 的合法前缀。
     */
    @Test
    void doesNotTreatTextPrefixAsPackageBoundary() {
        PackageMatcher matcher = new PackageMatcher(List.of("com.acme.entity"));

        assertFalse(matcher.matches("com.acme.en"));
        assertFalse(matcher.matches("com.acme.entityX"));
    }

    @Test
    void supportsMultiplePackages() {
        PackageMatcher matcher = new PackageMatcher(List.of("com.acme.entity", "zzz.other"));

        assertTrue(matcher.matches("com.acme.entity"));
        assertTrue(matcher.matches("zzz.other"));
        assertFalse(matcher.matches("com.acme"));
    }

    @Test
    void emptyOrBlankPackagesMatchNothing() {
        assertFalse(new PackageMatcher(List.of()).matches("com.acme"));
        assertFalse(new PackageMatcher(List.of("   ")).matches("com.acme"));
        assertFalse(new PackageMatcher(null).matches("com.acme"));
    }

    @Test
    void normalizesTrailingDotWhitespaceAndNullElements() {
        // 用 Arrays.asList 以便显式包含 null（List.of 不允许 null 元素）
        PackageMatcher matcher = new PackageMatcher(Arrays.asList("  com.acme.entity.  ", "", null));

        assertEquals(java.util.Set.of("com.acme.entity"), matcher.configuredPackages());
        assertTrue(matcher.matches("com.acme.entity"));
    }
}
