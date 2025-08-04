package com.abc.entity.web.exception;

import com.abc.entity.web.support.ExceptionCode;
import com.abc.entity.web.support.MessageFormatter;
import com.abc.entity.web.support.enums.DefaultExceptionCode;
import com.abc.entity.web.support.message.MessagePlaceholder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final ExceptionCode code;

    private final MessagePlaceholder messagePlaceholder;

    private static final MessageFormatter MESSAGE_FORMATTER = MessageFormatter.java();

    public BaseException(ExceptionCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.messagePlaceholder = null;
    }

    public BaseException(ExceptionCode code, MessagePlaceholder placeholder, Throwable cause) {
        super(MESSAGE_FORMATTER.format(placeholder.getPattern(), placeholder.getArgs()), cause);
        this.code = code;
        this.messagePlaceholder = placeholder;
    }

    public BaseException(ExceptionCode code, String message) {
        this(code, message, null);
    }

    public BaseException(String message) {
        this(DefaultExceptionCode.COMMON_ERROR, message);
    }

    public BaseException(MessagePlaceholder placeholder) {
        this(DefaultExceptionCode.COMMON_ERROR, placeholder, null);
    }


    public static BaseException common(String message) {
        return new BaseException(message);
    }


    public static BaseException common(MessagePlaceholder message) {
        return new BaseException(message);
    }


}
