package io.github.youngerier.support.web;

import io.github.youngerier.support.Response;
import io.github.youngerier.support.exception.BaseException;
import io.github.youngerier.support.exception.DefaultExceptionCode;
import io.github.youngerier.support.exception.ExceptionCode;
import io.github.youngerier.support.exception.ExceptionLogLevel;
import io.github.youngerier.support.i18n.ExceptionMessageProvider;
import io.github.youngerier.support.i18n.ExceptionMessageResolver;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器，是异常体系的边缘组件，负责把 {@link BaseException} 转换成两种视图：
 *
 * <ul>
 *     <li><b>用户视图</b>（HTTP 响应体 message）：按请求 {@code Accept-Language} 解析。
 *     friendly 异常只返回异常码对应的通用提示，真实细节不暴露。</li>
 *     <li><b>系统视图</b>（服务端日志）：携带错误码与可读详情，按 {@link ExceptionLogLevel}
 *     分级；i18n 异常按系统语言解析，ERROR 级别输出完整堆栈。</li>
 * </ul>
 *
 * <p>消息模板来源顺序由 {@link ExceptionMessageResolver} 决定：
 * 应用 {@link MessageSource} → {@link ExceptionMessageProvider}（如数据库）→
 * toolkit 内置中英文 bundle → 异常码 desc；占位符统一为 slf4j 风格的 {@code {}}。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ExceptionMessageResolver messageResolver;

    /**
     * 框架异常码：这类异常不携带 {@link ExceptionCode}，HTTP 状态由各 handler 决定，
     * 这里仅用于按请求语言解析消息。消息键按约定由枚举名自动生成，desc 为 bundle 缺失时的兜底模板。
     */
    private enum FrameworkErrorCode implements ExceptionCode {

        MISSING_PARAMETER("400", "缺少必填参数: {}"),
        TYPE_MISMATCH("400", "参数类型错误: {}"),
        MALFORMED_REQUEST("400", "请求体格式错误"),
        METHOD_NOT_SUPPORTED("405", "请求方法不支持: {}");

        private final String code;
        private final String desc;

        FrameworkErrorCode(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getDesc() {
            return desc;
        }
    }

    public GlobalExceptionHandler(MessageSource messageSource) {
        this(messageSource, List.of());
    }

    public GlobalExceptionHandler(MessageSource messageSource, List<ExceptionMessageProvider> providers) {
        this.messageResolver = new ExceptionMessageResolver(messageSource, providers);
    }

    // ---------------- 业务异常 ----------------

    /**
     * 业务异常：用户视图与系统视图分别处理
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Response<Void>> handleBaseException(BaseException ex) {
        logSystemView(ex);
        ExceptionCode code = ex.getCode();
        return ResponseEntity.status(code.httpStatus())
                .body(Response.error(toIntCode(code), toUserMessage(ex)));
    }

    // ---------------- 框架异常 ----------------

    /**
     * @RequestBody 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return badRequest(fieldErrorMessages(ex));
    }

    /**
     * 表单/对象绑定校验失败
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Response<Void>> handleBindException(BindException ex) {
        return badRequest(fieldErrorMessages(ex));
    }

    /**
     * 方法参数（@RequestParam / @PathVariable）校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Response<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        Locale locale = requestLocale();
        String message = ex.getConstraintViolations().stream()
                .map(violation -> resolveValidationMessage(violation.getMessage(), locale))
                .collect(Collectors.joining("; "));
        return badRequest(message);
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Response<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return badRequest(resolveCodeMessage(
                FrameworkErrorCode.MISSING_PARAMETER, requestLocale(), ex.getParameterName()));
    }

    /**
     * 参数类型不匹配，例如 ?id=abc 但 id 为 Long
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Response<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return badRequest(resolveCodeMessage(
                FrameworkErrorCode.TYPE_MISMATCH, requestLocale(), ex.getName()));
    }

    /**
     * 请求体不可读（JSON 格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return badRequest(resolveCodeMessage(FrameworkErrorCode.MALFORMED_REQUEST, requestLocale()));
    }

    /**
     * 上传文件超过大小限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Response<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Response.error(HttpStatus.PAYLOAD_TOO_LARGE.value(),
                        resolveCodeMessage(DefaultExceptionCode.PAYLOAD_TOO_LARGE, requestLocale())));
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Response<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        Set<HttpMethod> supported = ex.getSupportedHttpMethods();
        if (supported != null && !supported.isEmpty()) {
            // RFC 7231 要求 405 必须携带 Allow 头。Spring 的 DefaultHandlerExceptionResolver 会设置，
            // 但本处理器优先级更高，必须自己补上
            builder.allow(supported.toArray(new HttpMethod[0]));
        }
        return builder.body(Response.error(HttpStatus.METHOD_NOT_ALLOWED.value(),
                resolveCodeMessage(FrameworkErrorCode.METHOD_NOT_SUPPORTED, requestLocale(), ex.getMethod())));
    }

    /**
     * 静态资源 / 接口不存在（Spring 6 抛出）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Response<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Response.error(HttpStatus.NOT_FOUND.value(),
                        resolveCodeMessage(DefaultExceptionCode.NOT_FOUND, requestLocale())));
    }

    /**
     * 兜底异常：用户只见通用提示，真实异常（含堆栈）只进系统日志
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleException(Exception ex) {
        // Spring 自带的 ErrorResponse（ResponseStatusException、HttpMediaTypeNotSupportedException(415)、
        // HttpMediaTypeNotAcceptableException(406)、AsyncRequestTimeoutException(503)、
        // MissingServletRequestPartException(400) 等）自带语义正确的状态码与响应头。
        // 本处理器由 ExceptionHandlerExceptionResolver 执行，优先级高于
        // ResponseStatusExceptionResolver 与 DefaultHandlerExceptionResolver，
        // 若不在这里让位，消费方 throw new ResponseStatusException(CONFLICT) 会被统一吞成 500。
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode statusCode = errorResponse.getStatusCode();
            log.warn("Request rejected with status {}: {}", statusCode.value(), ex.getMessage());
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(statusCode);
            errorResponse.getHeaders().forEach((name, values) ->
                    values.forEach(value -> builder.header(name, value)));
            // ResponseStatusException 的 reason 是调用方显式写给客户端看的，优先保留；
            // 其余 ErrorResponse（415/406/503 等）按状态码映射内置文案
            String message = ex instanceof ResponseStatusException statusException
                    && StringUtils.hasText(statusException.getReason())
                    ? statusException.getReason()
                    : statusMessage(statusCode);
            return builder.body(Response.error(statusCode.value(), message));
        }
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.error(resolveCodeMessage(
                        DefaultExceptionCode.INTERNAL_SERVER_ERROR, requestLocale())));
    }

    // ---------------- 两种视图 ----------------

    /**
     * 用户视图：按请求语言生成响应消息。
     * <ul>
     *     <li>friendly：只返回异常码对应的通用文案，绝不携带具体参数/内部细节</li>
     *     <li>i18n：按消息键 + 请求语言解析模板并替换参数</li>
     *     <li>字面量：原样返回（调用方自行保证文案可对外展示）</li>
     * </ul>
     */
    private String toUserMessage(BaseException ex) {
        if (ex.isFriendly()) {
            // friendly：用户只见异常码对应的通用文案（随请求语言），异常码同时决定 HTTP 状态；
            // 真实细节只进系统日志
            return resolveCodeMessage(ex.getCode(), requestLocale());
        }
        if (ex.isI18n()) {
            return resolveCodeMessage(ex.getCode(), requestLocale(), ex.getMessageArgs());
        }
        return ex.getMessage();
    }

    /**
     * 系统视图：日志给开发排查用，必须包含错误码和可读详情。
     * i18n 异常按系统语言解析出完整消息（含参数），避免日志里只有消息键。
     */
    private void logSystemView(BaseException ex) {
        String detail = ex.isI18n()
                ? resolveCodeMessage(ex.getCode(), Locale.getDefault(), ex.getMessageArgs())
                : ex.getMessage();
        String line = "Business exception [{}]: {}";
        switch (ex.getLogLevel()) {
            case NONE -> {
            }
            case INFO -> log.info(line, ex.getTextCode(), detail);
            case WARN -> log.warn(line, ex.getTextCode(), detail);
            case ERROR -> log.error(line, ex.getTextCode(), detail, ex);
        }
    }

    private Locale requestLocale() {
        return LocaleContextHolder.getLocale();
    }

    private ResponseEntity<Response<Void>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Response.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    private String fieldErrorMessages(BindException ex) {
        Locale locale = requestLocale();
        return ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    // 校验注解 message 可写为消息键，由 MessageSource 解析；解析不了返回注解原文
                    String resolved = resolveValidationMessage(error.getDefaultMessage(), locale);
                    return error.getField() + ": " + resolved;
                })
                .collect(Collectors.joining("; "));
    }

    /**
     * 解析校验注解上的消息（可能是消息键，也可能是字面量，解析不了时原样返回）。
     * 字段校验与方法参数校验共用，保证两条路径行为一致。
     */
    private String resolveValidationMessage(String message, Locale locale) {
        return messageResolver.resolve(message, null, locale, message);
    }

    /**
     * 按请求语言解析异常码对应消息；无消息键或 bundle 未命中时回退 desc。
     */
    private String resolveCodeMessage(ExceptionCode code, Locale locale, Object... args) {
        String key = code.getMessageKey();
        if (key != null) {
            return messageResolver.resolve(key, args, locale, code.getDesc());
        }
        return code.getDesc();
    }

    private int toIntCode(ExceptionCode code) {
        try {
            return Integer.parseInt(code.getCode());
        } catch (NumberFormatException e) {
            // 非数字码：响应体 code 与实际返回的 HTTP 状态保持一致，避免两个视图自相矛盾
            return code.httpStatus();
        }
    }

    /**
     * 把 HTTP 状态码映射到内置异常码文案；未覆盖的状态码回退到 Spring 的 reason phrase。
     */
    private String statusMessage(HttpStatusCode statusCode) {
        DefaultExceptionCode mapped = switch (statusCode.value()) {
            case 400 -> DefaultExceptionCode.BAD_REQUEST;
            case 401 -> DefaultExceptionCode.UNAUTHORIZED;
            case 403 -> DefaultExceptionCode.FORBIDDEN;
            case 404 -> DefaultExceptionCode.NOT_FOUND;
            case 409 -> DefaultExceptionCode.CONFLICT;
            case 413 -> DefaultExceptionCode.PAYLOAD_TOO_LARGE;
            case 429 -> DefaultExceptionCode.TOO_MANY_REQUESTS;
            case 503 -> DefaultExceptionCode.SERVICE_UNAVAILABLE;
            default -> null;
        };
        if (mapped != null) {
            return resolveCodeMessage(mapped, requestLocale());
        }
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        return resolved != null ? resolved.getReasonPhrase() : String.valueOf(statusCode.value());
    }
}
