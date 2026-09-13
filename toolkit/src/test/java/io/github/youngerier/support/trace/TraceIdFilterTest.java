package io.github.youngerier.support.trace;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void clearMdc() {
        TraceContext.clear();
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
}
