package io.github.youngerier.support.trace;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesTraceIdAndEchoesHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] mdcDuringChain = new String[1];

        FilterChain chain = (req, res) -> mdcDuringChain[0] = TraceContext.getTraceId();
        filter.doFilter(request, response, chain);

        String traceId = response.getHeader(TraceContext.TRACE_ID_HEADER);
        assertNotNull(traceId);
        assertEquals(32, traceId.length());
        // 过滤器内 MDC 已设置，过滤器结束后已清理
        assertSame(traceId, mdcDuringChain[0]);
        assertNull(TraceContext.getTraceId());
    }

    @Test
    void reusesIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContext.TRACE_ID_HEADER, "upstream-trace-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertEquals("upstream-trace-id", response.getHeader(TraceContext.TRACE_ID_HEADER));
    }

    @Test
    void acceptsIncomingRequestIdAtMaxLength() throws Exception {
        String traceId = "a".repeat(TraceContext.MAX_TRACE_ID_LENGTH);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContext.TRACE_ID_HEADER, traceId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertEquals(traceId, response.getHeader(TraceContext.TRACE_ID_HEADER));
    }

    // ---------------- 上游 header 白名单校验（回归：日志伪造 / 超长放大） ----------------

    @Test
    void ignoresIncomingRequestIdWithLineBreaks() throws Exception {
        assertIllegalHeaderRegenerated("bad-id\r\nINFO forged log line");
    }

    @Test
    void ignoresIncomingRequestIdWithIllegalCharacters() throws Exception {
        assertIllegalHeaderRegenerated("<script>alert(1)</script>");
    }

    @Test
    void ignoresOverlongIncomingRequestId() throws Exception {
        assertIllegalHeaderRegenerated("a".repeat(TraceContext.MAX_TRACE_ID_LENGTH + 1));
    }

    @Test
    void ignoresBlankIncomingRequestId() throws Exception {
        assertIllegalHeaderRegenerated("   ");
    }

    /**
     * 回归：过滤器结束时必须「还原」MDC 而非只删 traceId，
     * 不能把上游 tracing agent（Brave/Micrometer 等）写入的 traceId 一起删掉。
     */
    @Test
    void doesNotRemoveTraceIdSetByUpstreamAgent() throws Exception {
        TraceContext.setTraceId("agent-managed-id");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertEquals("agent-managed-id", TraceContext.getTraceId());
    }

    private void assertIllegalHeaderRegenerated(String headerValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContext.TRACE_ID_HEADER, headerValue);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        String traceId = response.getHeader(TraceContext.TRACE_ID_HEADER);
        assertNotNull(traceId);
        assertEquals(32, traceId.length());
        assertNotEquals(headerValue, traceId);
    }
}
