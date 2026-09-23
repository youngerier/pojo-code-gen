package io.github.youngerier.support.trace;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 为每个 HTTP 请求准备 traceId：优先复用上游传入的合法请求头（默认 X-Request-ID，可配置），
 * 否则生成新的 traceId 放入 MDC，并回写到响应头，请求结束后清理。
 * <p>
 * 上游 header 必须匹配 {@code [A-Za-z0-9_-]{1,128}}，非法内容（含换行、超长、特殊字符）
 * 一律丢弃并重新生成，避免日志伪造和响应头污染。
 * <p>
 * 支持异步 Servlet（Callable / DeferredResult 等）：初始 REQUEST dispatch 的 traceId
 * 会存入 request 属性，ASYNC dispatch 时在新的执行线程上恢复 MDC。
 */
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * 接受的上游 traceId 最大长度
     */
    public static final int MAX_TRACE_ID_LENGTH = 128;

    /**
     * 合法 traceId 字符集：字母、数字、下划线、短横线，长度 1~{@value #MAX_TRACE_ID_LENGTH}
     */
    private static final Pattern VALID_TRACE_ID =
            Pattern.compile("[A-Za-z0-9_-]{1," + MAX_TRACE_ID_LENGTH + "}");

    private static final String TRACE_ID_ATTRIBUTE = TraceIdFilter.class.getName() + ".TRACE_ID";

    private final String headerName;

    public TraceIdFilter() {
        this(TraceContext.TRACE_ID_HEADER);
    }

    public TraceIdFilter(String headerName) {
        this.headerName = headerName;
    }

    /**
     * ASYNC dispatch 也需要执行本过滤器，以便在异步执行线程上恢复 traceId。
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId;
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            // 初始 REQUEST dispatch 已结束、其线程 MDC 已清理，从 request 属性恢复；
            // 属性缺失（无前置 REQUEST dispatch 的非常规入口）时兜底按请求头解析
            traceId = (String) request.getAttribute(TRACE_ID_ATTRIBUTE);
            if (traceId == null) {
                traceId = resolveTraceId(request);
            }
        } else {
            traceId = resolveTraceId(request);
            request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        }

        TraceContext.setTraceId(traceId);
        response.setHeader(headerName, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(headerName);
        if (header != null) {
            String candidate = header.trim();
            if (VALID_TRACE_ID.matcher(candidate).matches()) {
                return candidate;
            }
            if (!candidate.isEmpty() && logger.isDebugEnabled()) {
                logger.debug("Illegal incoming traceId header ignored, header=" + headerName);
            }
        }
        return TraceContext.generateTraceId();
    }
}
