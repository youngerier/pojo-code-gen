package io.github.youngerier.support.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 为每个 HTTP 请求准备 traceId：优先复用上游传入的 X-Request-ID，
 * 否则生成新的 traceId 放入 MDC，并回写到响应头，请求结束后清理。
 */
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        TraceContext.setTraceId(traceId);
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(TraceContext.TRACE_ID_HEADER);
        return StringUtils.hasText(header) ? header.trim() : TraceContext.generateTraceId();
    }
}
