package io.github.youngerier.support.autoconfigure;

import io.github.youngerier.support.trace.TraceIdFilter;
import io.github.youngerier.support.web.GlobalExceptionHandler;
import io.github.youngerier.support.web.SecurityExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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
 * 在最小 Servlet Web 上下文中验证自动装配真正注册了默认 Bean，
 * 并守住两类「消费者应用启动失败」缺陷的回归：
 *
 * <ul>
 *     <li>{@code RocketMqAutoConfiguration} 在 classpath 有 rocketmq-spring、但容器里没有
 *     {@code RocketMQTemplate} Bean 时不得尝试注入它（否则 NoSuchBeanDefinitionException）。</li>
 *     <li>{@code WebSupportAutoConfiguration} 在 classpath 无 spring-security、且注解元数据以
 *     <b>反射</b>方式读取时，不得实例化 {@link SecurityExceptionHandler}
 *     （否则 NoClassDefFoundError）。</li>
 * </ul>
 */
class AutoConfigurationTest {

    @Test
    void webSupportRegistersExceptionHandlerAndTraceFilter() {
        try (AnnotationConfigWebApplicationContext context = refreshWeb(WebSupportAutoConfiguration.class)) {
            assertNotNull(context.getBean(GlobalExceptionHandler.class));
            FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
            assertTrue(registration.getFilter() instanceof TraceIdFilter);
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
     * 从而复现真实缺陷：方法级 {@code @ConditionalOnClass} 在反射路径下被静默跳过。
     */
    @Test
    void webSupportStartsWithoutSpringSecurityUnderReflectiveMetadata() {
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
            assertTrue(context.containsBean("traceIdFilter"), "traceId 过滤器应当装配");
            assertFalse(context.containsBean("securityExceptionHandler"),
                    "无 spring-security 时不得装配 SecurityExceptionHandler");
        }
    }

    /**
     * 回归：classpath 有 rocketmq-spring（本模块 optional 依赖，测试期存在）、
     * 但容器中没有 {@code RocketMQTemplate} Bean 时必须正常启动，且不注册 {@code RocketMqProducer}。
     *
     * <p>对应缺陷：只判断 {@code @ConditionalOnClass(RocketMQTemplate.class)} 时，
     * rocketmq-spring 因缺少 {@code rocketmq.name-server} 而未创建 {@code RocketMQTemplate}，
     * 本模块仍尝试注入它，导致使用方应用启动失败。
     */
    @Test
    void rocketMqProducerSkippedWhenNoRocketMQTemplateBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RocketMqAutoConfiguration.class);
            assertDoesNotThrow(context::refresh);

            assertFalse(context.containsBean("rocketMqProducer"),
                    "容器中没有 RocketMQTemplate Bean 时不得装配 RocketMqProducer");
            // 链路 Hook 仍然应当装配：它们只依赖 rocketmq-client，不依赖 RocketMQTemplate
            assertTrue(context.containsBean("traceSendMessageHook"));
            assertTrue(context.containsBean("traceConsumeMessageHook"));
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
