package io.github.youngerier.support.web.autoconfigure;

import io.github.youngerier.support.web.GlobalExceptionHandler;
import io.github.youngerier.support.web.SecurityExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在最小 Servlet Web 上下文中验证 {@link WebSupportAutoConfiguration} 的装配行为，
 * 并守住「使用方应用启动失败」缺陷的回归。
 *
 * <p>traceId 过滤器的装配由 {@code toolkit-trace} 的 {@code TraceAutoConfiguration} 负责，
 * 对应测试在 trace 模块中。
 */
class WebSupportAutoConfigurationTest {

    @Test
    void registersGlobalExceptionHandler() {
        try (AnnotationConfigWebApplicationContext context = refreshWeb(WebSupportAutoConfiguration.class)) {
            assertNotNull(context.getBean(GlobalExceptionHandler.class));
        }
    }

    @Test
    void securityHandlerRegisteredWhenSpringSecurityPresent() {
        try (AnnotationConfigWebApplicationContext context = refreshWeb(WebSupportAutoConfiguration.class)) {
            assertNotNull(context.getBean(SecurityExceptionHandler.class));
        }
    }

    /**
     * 回归：没有 spring-security 时，即使注解元数据走反射路径，上下文也必须能正常启动，
     * 且不得注册 {@link SecurityExceptionHandler}。
     *
     * <p>用一个「隐藏 spring-security」的 ClassLoader 强制加载自动配置类的新 Class 实例，
     * 从而复现真实缺陷：方法级 {@code @ConditionalOnClass} 在反射路径下被静默跳过，
     * {@code SecurityExceptionHandler} 仍被创建，随后内省其 {@code @ExceptionHandler}
     * 注解时抛 {@code NoClassDefFoundError}。
     */
    @Test
    void startsWithoutSpringSecurityUnderReflectiveMetadata() {
        ClassLoader hiding = new HidingClassLoader(getClass().getClassLoader());
        Class<?> config = assertDoesNotThrow(
                () -> Class.forName(WebSupportAutoConfiguration.class.getName(), false, hiding));
        // 确认确实拿到了由隐藏 ClassLoader 加载的另一份 Class，即注解元数据将以反射方式读取
        assertNotSame(WebSupportAutoConfiguration.class, config);

        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setClassLoader(hiding);
            context.setServletContext(new MockServletContext());
            context.register(config);
            assertDoesNotThrow(context::refresh);

            assertTrue(context.containsBean("globalExceptionHandler"), "全局异常处理器应当装配");
            assertFalse(context.containsBean("securityExceptionHandler"),
                    "无 spring-security 时不得装配 SecurityExceptionHandler");
        }
    }

    private AnnotationConfigWebApplicationContext refreshWeb(Class<?>... configurations) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(configurations);
        context.refresh();
        return context;
    }

    /**
     * 对 {@code org.springframework.security.} 前缀一律抛 ClassNotFoundException，
     * 并对本模块自身的类做「子优先」加载（定义新副本），
     * 用于在 spring-security 实际仍在测试 classpath 上时模拟「使用方没有引入 spring-security」，
     * 同时确保自动配置类确实由本 ClassLoader 重新加载、注解元数据走反射路径。
     */
    private static final class HidingClassLoader extends ClassLoader {

        private static final Set<String> HIDDEN_PREFIXES = Set.of("org.springframework.security.");

        /** 本模块自身的类：子优先，以便得到与父加载器不同的 Class 实例 */
        private static final String RELOAD_PREFIX = "io.github.youngerier.support.";

        private HidingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            for (String prefix : HIDDEN_PREFIXES) {
                if (name.startsWith(prefix)) {
                    throw new ClassNotFoundException(name);
                }
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = name.startsWith(RELOAD_PREFIX)
                            ? defineLocally(name)
                            : super.loadClass(name, false);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        private Class<?> defineLocally(String name) throws ClassNotFoundException {
            String resource = name.replace('.', '/') + ".class";
            try (InputStream in = getParent().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = in.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
