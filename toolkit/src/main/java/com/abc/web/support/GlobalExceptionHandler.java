package com.abc.web.support;

import com.abc.web.support.enums.I18nCommonExceptionCode;
import com.abc.web.support.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中的各种异常，并返回国际化的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final I18nMessageService messageService;

    public GlobalExceptionHandler(I18nMessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Response<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage(), e);
        
        String message = getLocalizedMessage(e);
        Response<Void> response = Response.error(
                Integer.parseInt(e.getCode()),
                message
        );
        
        return ResponseEntity.ok(response);
    }



    /**
     * 处理系统异常
     */
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<Response<Void>> handleSystemException(SystemException e) {
        log.error("System exception: {}", e.getMessage(), e);
        
        String message = getLocalizedMessage(e);
        Response<Void> response = Response.error(
                Integer.parseInt(e.getCode()),
                message
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }



    /**
     * 处理参数验证异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Method argument not valid: {}", e.getMessage());
        
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        Response<Void> response = Response.error(
                Integer.parseInt(I18nCommonExceptionCode.VALIDATION_ERROR.getCode()),
                message
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Response<Void>> handleBindException(BindException e) {
        log.warn("Bind exception: {}", e.getMessage());
        
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        Response<Void> response = Response.error(
                Integer.parseInt(I18nCommonExceptionCode.VALIDATION_ERROR.getCode()),
                message
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Response<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("Constraint violation: {}", e.getMessage());
        
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        
        Response<Void> response = Response.error(
                Integer.parseInt(I18nCommonExceptionCode.VALIDATION_ERROR.getCode()),
                message
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        
        Response<Void> response = Response.error(
                Integer.parseInt(I18nCommonExceptionCode.BAD_REQUEST.getCode()),
                e.getMessage()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleException(Exception e) {
        log.error("Unexpected exception: {}", e.getMessage(), e);
        
        String message = messageService.getExceptionMessage(I18nCommonExceptionCode.INTERNAL_SERVER_ERROR);
        Response<Void> response = Response.error(
                Integer.parseInt(I18nCommonExceptionCode.INTERNAL_SERVER_ERROR.getCode()),
                message
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 获取本地化异常消息
     */
    private String getLocalizedMessage(BaseException exception) {
        return ExceptionUtils.getLocalizedMessage(exception);
    }
}