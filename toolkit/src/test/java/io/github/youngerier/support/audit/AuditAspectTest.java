package io.github.youngerier.support.audit;

import io.github.youngerier.support.audit.annotations.Auditable;
import io.github.youngerier.support.audit.annotations.IgnoreParam;
import io.github.youngerier.support.audit.annotations.SensitiveParam;
import io.github.youngerier.support.audit.annotations.SensitiveParam.MaskStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 不启动 Spring 容器，直接用 AspectJProxyFactory 验证审计切面核心行为。
 */
class AuditAspectTest {

    private final List<AuditEvent> events = new ArrayList<>();
    private TestService proxy;

    @BeforeEach
    void setUp() {
        AuditAspect aspect = new AuditAspect(events::add, () -> "user-1", null, Runnable::run);
        AspectJProxyFactory factory = new AspectJProxyFactory(new TestService());
        factory.addAspect(aspect);
        proxy = factory.getProxy();
    }

    @Test
    void recordsOperationWithRealParameterNameInSpel() {
        proxy.update(42L, "name");

        assertEquals(1, events.size());
        AuditEvent event = events.get(0);
        assertEquals("update", event.getOperation());
        assertEquals("42", event.getBusinessKey());
        assertEquals("user-1", event.getUserId());
        assertTrue(event.isSuccess());
        // 未注入 ObjectMapper 时使用 Map.toString 回退，仍能验证真实参数名
        assertTrue(event.getParameters().contains("id=42"));
    }

    @Test
    void masksSensitiveParameter() {
        proxy.login("13812348888");

        assertEquals("138****8888", events.get(0).getParameters().replaceAll(".*(138\\*\\*\\*\\*8888).*", "$1"));
    }

    @Test
    void ignoresAnnotatedParameter() {
        proxy.changePassword("top-secret-token", "new-pwd");

        String params = events.get(0).getParameters();
        assertTrue(params.contains("***"));
        assertFalse(params.contains("top-secret-token"));
    }

    @Test
    void skipsAuditWhenConditionIsFalse() {
        proxy.maybeAudit(false);
        assertTrue(events.isEmpty());

        proxy.maybeAudit(true);
        assertEquals(1, events.size());
    }

    @Test
    void recordsFailureAndRethrows() {
        assertThrows(IllegalStateException.class, () -> proxy.fail());

        AuditEvent event = events.get(0);
        assertFalse(event.isSuccess());
        assertEquals("boom", event.getErrorMessage());
    }

    @Test
    void resultNotRecordedByDefault() {
        proxy.find(1L);
        assertNull(events.get(0).getResult());
    }

    @SuppressWarnings("unused")
    static class TestService {

        @Auditable(operation = "update", businessKey = "#id")
        public void update(Long id, String name) {
        }

        @Auditable(operation = "login")
        public void login(@SensitiveParam(strategy = MaskStrategy.PHONE) String phone) {
        }

        @Auditable(operation = "changePassword")
        public void changePassword(@IgnoreParam String token, String newPassword) {
        }

        @Auditable(operation = "maybe", condition = "#enabled")
        public void maybeAudit(boolean enabled) {
        }

        @Auditable(operation = "fail")
        public void fail() {
            throw new IllegalStateException("boom");
        }

        @Auditable(operation = "find")
        public String find(Long id) {
            return "found";
        }
    }
}
