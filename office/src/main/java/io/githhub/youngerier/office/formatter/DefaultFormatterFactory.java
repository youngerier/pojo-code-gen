package io.githhub.youngerier.office.formatter;

import io.github.youngerier.support.AssertUtils;
import io.github.youngerier.support.DateFormatPatterns;
import io.github.youngerier.support.constants.Constants;
import io.github.youngerier.support.enums.DateFormater;
import io.github.youngerier.support.enums.DescriptiveEnum;
import io.github.youngerier.support.exception.BaseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.Formatter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;

/**
 * {@link Formatter} 默认工厂
 *
 **/
public final class DefaultFormatterFactory {

    private static final Map<String, DateTimeFormatter> DEFAULT_FORMATTERS = new HashMap<>();

    static {
        DEFAULT_FORMATTERS.put(DateFormatPatterns.ISO_8601_EXTENDED_DATETIME, DateFormater.ISO_8601_EXTENDED_DATETIME.getFormatter());
        DEFAULT_FORMATTERS.put(DateFormatPatterns.YYYY_MM_DD_HH_MM_SS, DateFormater.YYYY_MM_DD_HH_MM_SS.getFormatter());
        DEFAULT_FORMATTERS.put(DateFormatPatterns.YYYY_MM_DD_HH_MM, DateFormater.YYYY_MM_DD_HH_MM.getFormatter());
        DEFAULT_FORMATTERS.put(DateFormatPatterns.YYYY_MM_DD_HH, DateFormater.YYYY_MM_DD_HH.getFormatter());
        DEFAULT_FORMATTERS.put(DateFormatPatterns.YYYY_MM_DD, DateFormater.YYYY_MM_DD.getFormatter());
        DEFAULT_FORMATTERS.put(DateFormatPatterns.YYYY_MM, DateFormater.YYYY_MM.getFormatter());
        DEFAULT_FORMATTERS.put(DateFormatPatterns.YYYY, DateFormater.YYYY.getFormatter());
    }

    private DefaultFormatterFactory() {
        throw new AssertionError();
    }

    public static Formatter<Boolean> ofBool(String trueDesc, String falseDesc) {
        return new MapFormatter<>(Map.of(Constants.TRUE, trueDesc, Boolean.FALSE.toString(), falseDesc));
    }

    public static <T extends DescriptiveEnum> Formatter<T> ofEnum(Class<T> enumsClass) {
        AssertUtils.isTrue(enumsClass.isEnum(), "argument enumsClass must enum type");
        DescriptiveEnum[] enumConstants = enumsClass.getEnumConstants();
        HashMap<String, Object> source = new HashMap<>();
        for (DescriptiveEnum e : enumConstants) {
            source.put(e.name(), e.getDesc());
        }
        return new MapFormatter<>(source);
    }

    public static Formatter<TemporalAccessor> ofDateTime(String pattern) {
        DateTimeFormatter formatter = DEFAULT_FORMATTERS.containsKey(pattern) ? DEFAULT_FORMATTERS.get(pattern) :
                DateTimeFormatter.ofPattern(pattern);
        return new Formatter<TemporalAccessor>() {

            @Override
            public TemporalAccessor parse(String text, Locale locale) throws ParseException {
                return StringUtils.isNotEmpty(text) ? formatter.parse(text) : null;
            }

            @Override
            @NonNull
            public String print(@NonNull TemporalAccessor time, @Nullable Locale locale) {
                return formatter.format(time);
            }
        };
    }

    public static Formatter<Object[]> ofArray() {
        return new Formatter<Object[]>() {

            @Override
            public Object[] parse(String text, Locale locale) throws ParseException {
                throw BaseException.common("unsupported, Please handle it yourself.");
            }

            @Override
            @NonNull
            public String print(@NonNull Object[] values, @Nullable Locale locale) {
                return StringUtils.join(values, ",");
            }
        };
    }

    public static Formatter<Collection<?>> ofCollection() {
        return new Formatter<Collection<?>>() {

            @Override
            public Collection<?> parse(String text, Locale locale) throws ParseException {
                return Arrays.stream(StringUtils.split(text, ",")).toList();
            }

            @Override
            public String print(@NonNull Collection<?> object, @Nullable Locale locale) {
                return StringUtils.join(object, ",");
            }
        };
    }
}
