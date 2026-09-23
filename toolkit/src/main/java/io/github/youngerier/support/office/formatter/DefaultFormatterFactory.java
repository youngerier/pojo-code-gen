package io.github.youngerier.support.office.formatter;

import io.github.youngerier.support.AssertUtils;
import io.github.youngerier.support.DateFormatPatterns;
import io.github.youngerier.support.constants.Constants;
import io.github.youngerier.support.enums.DateFormatter;
import io.github.youngerier.support.enums.DescriptiveEnum;
import io.github.youngerier.support.exception.BaseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.Formatter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
        DEFAULT_FORMATTERS.put(DateFormatPatterns.ISO_8601_EXTENDED_DATETIME, DateFormatter.ISO_8601_EXTENDED_DATETIME.getFormatter());
        DEFAULT_FORMATTERS.put(DateFormatPatterns.YYYY_MM_DD_HH_MM_SS, DateFormatter.YYYY_MM_DD_HH_MM_SS.getFormatter());
        DEFAULT_FORMATTERS.put(DateFormatPatterns.YYYY_MM_DD_HH_MM, DateFormatter.YYYY_MM_DD_HH_MM.getFormatter());
        DEFAULT_FORMATTERS.put(DateFormatPatterns.YYYY_MM_DD_HH, DateFormatter.YYYY_MM_DD_HH.getFormatter());
        DEFAULT_FORMATTERS.put(DateFormatPatterns.YYYY_MM_DD, DateFormatter.YYYY_MM_DD.getFormatter());
        DEFAULT_FORMATTERS.put(DateFormatPatterns.YYYY_MM, DateFormatter.YYYY_MM.getFormatter());
        DEFAULT_FORMATTERS.put(DateFormatPatterns.YYYY, DateFormatter.YYYY.getFormatter());
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
            source.put(((Enum<?>) e).name(), e.getDesc());
        }
        return new MapFormatter<>(source);
    }

    public static Formatter<TemporalAccessor> ofDateTime(String pattern) {
        DateTimeFormatter formatter = resolveDateTimeFormatter(pattern);
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

    /**
     * {@link java.util.Date} 专用 formatter。
     *
     * <p>{@link #ofDateTime(String)} 只能处理 {@link TemporalAccessor}，
     * 把 {@code java.util.Date} 字段交给它会在 print 时抛 {@code ClassCastException}。
     * 这里通过 {@link ZoneId#systemDefault()} 做转换，保持 {@link DateTimeFormatter} 的线程安全性。
     *
     * @param pattern 日期格式
     */
    public static Formatter<java.util.Date> ofDate(String pattern) {
        DateTimeFormatter formatter = resolveDateTimeFormatter(pattern);
        return new Formatter<java.util.Date>() {

            @Override
            public java.util.Date parse(String text, Locale locale) throws ParseException {
                if (StringUtils.isEmpty(text)) {
                    return null;
                }
                // 注意：pattern 需包含时间部分，否则请改用 ofDateTime 或自行解析
                return java.util.Date.from(LocalDateTime.parse(text, formatter)
                        .atZone(ZoneId.systemDefault())
                        .toInstant());
            }

            @Override
            @NonNull
            public String print(@NonNull java.util.Date date, @Nullable Locale locale) {
                return formatter.format(date.toInstant().atZone(ZoneId.systemDefault()));
            }
        };
    }

    private static DateTimeFormatter resolveDateTimeFormatter(String pattern) {
        return DEFAULT_FORMATTERS.containsKey(pattern)
                ? DEFAULT_FORMATTERS.get(pattern)
                : DateTimeFormatter.ofPattern(pattern);
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
