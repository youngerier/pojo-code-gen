package io.github.youngerier.support.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TracePropagatorTest {

    @AfterEach
    void clearMdc() {
        TraceContext.clear();
    }

    @Test
    void injectReusesCurrentTraceId() {
        TraceContext.setTraceId("current-id");
        Map<String, String> carrier = new HashMap<>();

        String traceId = TracePropagator.inject(carrier::put);

        assertEquals("current-id", traceId);
        assertEquals("current-id", carrier.get(TraceContext.TRACE_ID));
    }

    @Test
    void injectGeneratesTraceIdWhenAbsent() {
        Map<String, String> carrier = new HashMap<>();

        String traceId = TracePropagator.inject(carrier::put);

        assertEquals(32, traceId.length());
        assertEquals(traceId, carrier.get(TraceContext.TRACE_ID));
        assertEquals(traceId, TraceContext.getTraceId());
    }

    @Test
    void extractRestoresTraceIdFromCarrier() {
        String traceId = TracePropagator.extract(key -> "upstream-id");

        assertEquals("upstream-id", traceId);
        assertEquals("upstream-id", TraceContext.getTraceId());
    }

    @Test
    void extractGeneratesTraceIdWhenCarrierEmpty() {
        String traceId = TracePropagator.extract(key -> "  ");

        assertEquals(32, traceId.length());
        assertEquals(traceId, TraceContext.getTraceId());
    }

    @Test
    void runRestoresPreviousMdcAfterTask() {
        TraceContext.setTraceId("previous-id");

        TracePropagator.run(key -> "consumed-id", () ->
                assertEquals("consumed-id", TraceContext.getTraceId()));

        assertEquals("previous-id", TraceContext.getTraceId());
    }

    @Test
    void runClearsMdcWhenNoPreviousContext() {
        TracePropagator.run(key -> null, () ->
                assertEquals(32, TraceContext.getTraceId().length()));

        assertNull(MDC.get(TraceContext.TRACE_ID));
    }

    @Test
    void resolveTreatsBlankAsMissing() {
        assertNull(TracePropagator.resolve(key -> "   "));
        assertNull(TracePropagator.resolve(key -> null));
        assertEquals("id", TracePropagator.resolve(key -> "  id  "));
    }
}
