package com.abc.entity.web.support;

import java.text.MessageFormat;

public interface MessageFormatter {
    String format(String pattern, Object... args);


    static MessageFormatter java() {
        return MessageFormat::format;
    }

}
