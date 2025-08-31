package com.abc.web.support.exception.strategy;

import com.abc.web.support.ExceptionUtils;
import com.abc.web.support.enums.I18nCommonExceptionCode;
import com.abc.web.support.exception.ExceptionHandlerResult;
import com.abc.web.support.exception.ExceptionHandlerStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring验证异常处理策略
 * 处理Spring Validation框架的异常
 * 
 * 设计目标：
 * 1. 统一处理@Valid、@Validated等验证异常
 * 2. 提供清晰的字段级错误信息
 * 3. 支持多字段同时验证失败的场景
 * 4. 用户友好的错误消息格式
 * 
 * 开发体验：
 * - 自动收集所有验证失败的字段
 * - 提供结构化的错误信息
 * - 支持国际化错误消息
 * - 便于前端展示验证错误
 */
@Component
public class ValidationExceptionStrategy implements ExceptionHandlerStrategy {

    @Override
    public boolean supports(Exception exception) {
        return exception instanceof MethodArgumentNotValidException
                || exception instanceof ConstraintViolationException;
    }

    @Override
    public ExceptionHandlerResult handle(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodArgException) {
            return handleMethodArgumentNotValid(methodArgException);
        } else if (exception instanceof ConstraintViolationException constraintException) {
            return handleConstraintViolation(constraintException);
        }
        
        String message = ExceptionUtils.getLocalizedMessage(I18nCommonExceptionCode.VALIDATION_ERROR);
        return ExceptionHandlerResult.validation(I18nCommonExceptionCode.VALIDATION_ERROR);
    }

    /**
     * 处理方法参数验证异常（@RequestBody + @Valid）
     */
    private ExceptionHandlerResult handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        Map<String, Object> fieldErrors = new HashMap<>();
        
        // 收集字段级别的错误
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        // 收集全局错误
        exception.getBindingResult().getGlobalErrors().forEach(error -> {
            fieldErrors.put("_global", error.getDefaultMessage());
        });
        
        // 构建国际化消息
        String baseMessage = ExceptionUtils.getLocalizedMessage(I18nCommonExceptionCode.VALIDATION_ERROR);
        String message = String.format("%s，共 %d 个字段错误", baseMessage, fieldErrors.size());
        
        return ExceptionHandlerResult.validation(
                I18nCommonExceptionCode.VALIDATION_ERROR.getCode(),
                message
        ).toBuilder()
                .extra(Map.of("fieldErrors", fieldErrors))
                .details(buildDetailMessage(fieldErrors))
                .build();
    }

    /**
     * 处理约束违反异常（方法参数验证）
     */
    private ExceptionHandlerResult handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> violations = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (existing, replacement) -> existing // 保留第一个错误消息
                ));
        
        // 构建国际化消息
        String baseMessage = ExceptionUtils.getLocalizedMessage(I18nCommonExceptionCode.VALIDATION_ERROR);
        String message = String.format("%s，共 %d 个约束违反", baseMessage, violations.size());
        
        return ExceptionHandlerResult.validation(
                I18nCommonExceptionCode.VALIDATION_ERROR.getCode(),
                message
        ).toBuilder()
                .extra(Map.of("violations", violations))
                .details(String.join("; ", violations.values()))
                .build();
    }

    /**
     * 构建详细错误消息
     */
    private String buildDetailMessage(Map<String, Object> fieldErrors) {
        return fieldErrors.entrySet().stream()
                .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("; "));
    }

    @Override
    public int getPriority() {
        // 验证异常优先级最高，最先处理
        return 5;
    }
}