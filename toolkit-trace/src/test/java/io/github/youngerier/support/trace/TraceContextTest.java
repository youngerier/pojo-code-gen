package io.github.youngerier.support.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraceContextTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    // ---------------- sanitizeTraceId ----------------

    @Test
    void sanitizeAcceptsIdentifierCharactersAndTrims() {
        assertEquals("abc-123_4.5", TraceContext.sanitizeTraceId("  abc-123_4.5  "));
        assertEquals("a", TraceContext.sanitizeTraceId("a"));
        assertEquals("a".repeat(TraceContext.MAX_TRACE_ID_LENGTH),
                TraceContext.sanitizeTraceId("a".repeat(TraceContext.MAX_TRACE_ID_LENGTH)));
    }

    @Test
    void sanitizeRejectsNullBlankAndIllegalContent() {
        assertNull(TraceContext.sanitizeTraceId(null));
        assertNull(TraceContext.sanitizeTraceId(""));
        assertNull(TraceContext.sanitizeTraceId("   "));
        // 换行 / 控制字符：日志伪造
        assertNull(TraceContext.sanitizeTraceId("id\r\nINFO forged"));
        assertNull(TraceContext.sanitizeTraceId("id\nnext"));
        // 特殊字符
        assertNull(TraceContext.sanitizeTraceId("<script>"));
        assertNull(TraceContext.sanitizeTraceId("id with space"));
        assertNull(TraceContext.sanitizeTraceId("id;drop"));
        // 超长
        assertNull(TraceContext.sanitizeTraceId("a".repeat(TraceContext.MAX_TRACE_ID_LENGTH + 1)));
    }

    // ---------------- snapshot / restore ----------------

    @Test
    void snapshotAndRestoreRoundTrip() {
        MDC.put("bizKey", "biz-value");
        Map<String, String> snapshot = TraceContext.snapshot();

        TraceContext.setTraceId("temp-id");
        MDC.put("bizKey", "changed");

        TraceContext.restore(snapshot);

        assertNull(TraceContext.getTraceId());
        assertEquals("biz-value", MDC.get("bizKey"));
    }

    @Test
    void restoreNullSnapshotClearsMdc() {
        TraceContext.setTraceId("some-id");

        TraceContext.restore(null);

        assertNull(TraceContext.getTraceId());
    }

    // ---------------- wrap ----------------

    @Test
    void wrapRunsTaskWithSubmitterContextAndRestoresExecutionThread() {
        TraceContext.setTraceId("submitted-id");
        String[] seen = new String[1];
        Runnable wrapped = TraceContext.wrap(() -> seen[0] = TraceContext.getTraceId());

        // 模拟「借用的池线程上已有另一条任务的残留上下文」
        MDC.clear();
        TraceContext.setTraceId("other-id");

        wrapped.run();

        assertEquals("submitted-id", seen[0]);
        assertEquals("other-id", TraceContext.getTraceId(), "执行线程应恢复到运行前的状态");
    }

    /**
     * 回归：提交线程没有 MDC 时，执行线程上的残留上下文必须被清空，
     * 否则上一条任务的 traceId 会串进本次任务。
     */
    @Test
    void wrapClearsStaleExecutionThreadContextWhenSubmitterHasNone() {
        MDC.clear();
        String[] seen = new String[1];
        Runnable wrapped = TraceContext.wrap(() -> seen[0] = TraceContext.getTraceId());

        // 池线程上残留了上一轮任务的上下文
        TraceContext.setTraceId("stale-id");

        wrapped.run();

        assertNull(seen[0], "任务不得看到执行线程的残留 traceId");
    }

    @Test
    void restoresExecutionThreadContextEvenWhenTaskThrows() {
        TraceContext.setTraceId("submitted-id");
        Runnable wrapped = TraceContext.wrap(() -> {
            throw new IllegalStateException("boom");
        });

        MDC.clear();
        TraceContext.setTraceId("execution-thread-id");
        try {
            wrapped.run();
        } catch (IllegalStateException expected) {
            // 预期
        }

        assertEquals("execution-thread-id", TraceContext.getTraceId(),
                "任务抛异常时也必须还原执行线程的上下文");
    }

    @Test
    void wrapCallablePassesThroughResultAndRestores() throws Exception {
        TraceContext.setTraceId("callable-id");
        Callable<String> wrapped = TraceContext.wrapCallable(() -> "result-" + TraceContext.getTraceId());

        MDC.clear();
        TraceContext.setTraceId("other-id");

        assertEquals("result-callable-id", wrapped.call());
        assertEquals("other-id", TraceContext.getTraceId());
    }

    @Test
    void ensureTraceIdGeneratesNonBlankId() {
        MDC.clear();

        String traceId = TraceContext.ensureTraceId();

        assertEquals(32, traceId.length());
        assertEquals(traceId, TraceContext.getTraceId());
    }
}
