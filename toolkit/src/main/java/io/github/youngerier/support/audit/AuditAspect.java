package io.github.youngerier.support.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.youngerier.support.audit.annotations.Auditable;
import io.github.youngerier.support.audit.annotations.IgnoreParam;
import io.github.youngerier.support.audit.annotations.SensitiveParam;
import io.github.youngerier.support.trace.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 审计切面：拦截 {@link Auditable} 注解的方法，采集操作信息后交给 {@link AuditSink}。
 *
 * <p>采集动作尽量轻量（参数名反射结果由 Spring 缓存、SpEL 表达式本地缓存），
 * 输出默认异步执行，不阻塞业务线程。
 */
@Slf4j
@Aspect
public class AuditAspect {

    private static final int MAX_CONTENT_LENGTH = 4000;

    private final AuditSink sink;
    private final AuditUserProvider userProvider;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public AuditAspect(AuditSink sink, AuditUserProvider userProvider,
                       ObjectMapper objectMapper, Executor auditExecutor) {
        this.sink = sink;
        this.userProvider = userProvider;
        this.objectMapper = objectMapper;
        this.executor = auditExecutor;
    }

    @Around("@annotation(io.github.youngerier.support.audit.annotations.Auditable)"
            + " || @within(io.github.youngerier.support.audit.annotations.Auditable)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Auditable auditable = resolveAuditable(method, pjp.getTarget().getClass());
        if (auditable == null || !java.lang.reflect.Modifier.isPublic(method.getModifiers())
                || method.getDeclaringClass() == Object.class) {
            return pjp.proceed();
        }

        EvaluationContext spelContext = buildSpelContext(method, pjp.getArgs());
        if (!matchesCondition(auditable.condition(), spelContext)) {
            return pjp.proceed();
        }

        long start = System.nanoTime();
        Object result = null;
        Throwable thrown = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable ex) {
            thrown = ex;
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            publishEvent(auditable, method, pjp.getArgs(), result, thrown, durationMs, spelContext);
        }
    }

    private Auditable resolveAuditable(Method method, Class<?> targetClass) {
        Auditable onMethod = method.getAnnotation(Auditable.class);
        if (onMethod != null) {
            return onMethod;
        }
        return targetClass.getAnnotation(Auditable.class);
    }

    private EvaluationContext buildSpelContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] names = parameterNameDiscoverer.getParameterNames(method);
        for (int i = 0; i < args.length; i++) {
            context.setVariable("param" + i, args[i]);
            if (names != null && names[i] != null) {
                context.setVariable(names[i], args[i]);
            }
        }
        return context;
    }

    private boolean matchesCondition(String condition, EvaluationContext context) {
        if (!StringUtils.hasText(condition)) {
            return true;
        }
        try {
            Boolean value = expression(condition).getValue(context, Boolean.class);
            return !Boolean.FALSE.equals(value);
        } catch (Exception e) {
            log.warn("Failed to evaluate audit condition '{}', audit proceeds by default", condition, e);
            return true;
        }
    }

    private void publishEvent(Auditable auditable, Method method, Object[] args, Object result,
                              Throwable thrown, long durationMs, EvaluationContext spelContext) {
        try {
            AuditEvent.AuditEventBuilder builder = AuditEvent.builder()
                    .id(UUID.randomUUID().toString().replace("-", ""))
                    .time(Instant.now())
                    .traceId(TraceContext.getTraceId())
                    .userId(currentUser())
                    .operation(StringUtils.hasText(auditable.operation()) ? auditable.operation() : method.getName())
                    .type(auditable.type())
                    .businessKey(evalString(auditable.businessKey(), spelContext))
                    .parameters(auditable.includeParameters() ? serializeParameters(method, args) : null)
                    .result(auditable.includeResult() && thrown == null ? toJson(result) : null)
                    .success(thrown == null)
                    .errorMessage(thrown == null ? null : thrown.getMessage())
                    .durationMs(durationMs);
            RequestInfo requestInfo = currentRequestInfo();
            if (requestInfo != null) {
                builder.path(requestInfo.path())
                        .httpMethod(requestInfo.httpMethod())
                        .clientIp(requestInfo.clientIp());
            }
            AuditEvent event = builder.build();

            if (auditable.async() && executor != null) {
                executor.execute(() -> safeSave(event));
            } else {
                safeSave(event);
            }
        } catch (Exception e) {
            // 审计本身不能影响业务
            log.warn("Failed to build audit event for {}", method.getName(), e);
        }
    }

    private void safeSave(AuditEvent event) {
        try {
            sink.save(event);
        } catch (Exception e) {
            log.warn("Audit sink failed", e);
        }
    }

    private String currentUser() {
        try {
            return userProvider == null ? null : userProvider.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String evalString(String expression, EvaluationContext context) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        try {
            Object value = expression(expression).getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            log.warn("Failed to evaluate audit expression '{}'", expression, e);
            return null;
        }
    }

    private Expression expression(String expressionString) {
        return expressionCache.computeIfAbsent(expressionString, parser::parseExpression);
    }

    private String serializeParameters(Method method, Object[] args) {
        if (args.length == 0) {
            return null;
        }
        String[] names = parameterNameDiscoverer.getParameterNames(method);
        Map<String, Object> parameters = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String name = names != null && names[i] != null ? names[i] : "param" + i;
            parameters.put(name, maskIfNeeded(method.getParameterAnnotations()[i], args[i]));
        }
        return truncate(toJson(parameters));
    }

    private Object maskIfNeeded(Annotation[] annotations, Object value) {
        for (java.lang.annotation.Annotation annotation : annotations) {
            if (annotation instanceof IgnoreParam) {
                return "***";
            }
            if (annotation instanceof SensitiveParam sensitive) {
                return DataMaskingUtils.mask(value, sensitive.strategy(), sensitive.customExpression());
            }
        }
        // 默认排除不可序列化的 Web / 持久化基础设施对象，无需使用者逐个标注 @IgnoreParam
        return isInfrastructureValue(value) ? "<" + value.getClass().getSimpleName() + ">" : value;
    }

    private boolean isInfrastructureValue(Object value) {
        return value instanceof HttpServletRequest
                || value instanceof HttpServletResponse
                || value instanceof org.springframework.web.multipart.MultipartFile
                || value instanceof java.io.InputStream
                || value instanceof java.io.OutputStream
                || value instanceof org.springframework.validation.BindingResult;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return truncate(objectMapper != null ? objectMapper.writeValueAsString(value) : String.valueOf(value));
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_CONTENT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_CONTENT_LENGTH) + "...(truncated)";
    }

    private RequestInfo currentRequestInfo() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                HttpServletRequest request = attributes.getRequest();
                return new RequestInfo(request.getRequestURI(), request.getMethod(), resolveClientIp(request));
            }
        } catch (Exception ignored) {
            // 非 Web 环境忽略
        }
        return null;
    }

    private record RequestInfo(String path, String httpMethod, String clientIp) {
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp.trim() : request.getRemoteAddr();
    }
}
