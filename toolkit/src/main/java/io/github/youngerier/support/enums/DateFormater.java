package io.github.youngerier.support.enums;

import io.github.youngerier.support.DateFormatPatterns;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

@AllArgsConstructor
@Getter
public enum DateFormater {

    ISO_8601_EXTENDED_DATETIME(DateTimeFormatter.ofPattern(DateFormatPatterns.ISO_8601_EXTENDED_DATETIME)),

    YYYYMMDD(DateTimeFormatter.ofPattern(DateFormatPatterns.YYYYMMDD)),

    YYYYMMDDHH(DateTimeFormatter.ofPattern(DateFormatPatterns.YYYYMMDDHH)),

    YYYYMMDDHHMM(DateTimeFormatter.ofPattern(DateFormatPatterns.YYYYMMDDHHMM)),

    YYYYMMDDHHMMSS(DateTimeFormatter.ofPattern(DateFormatPatterns.YYYYMMDDHHMMSS)),

    YYYY_MM_DD_HH_MM_SS(DateTimeFormatter.ofPattern(DateFormatPatterns.YYYY_MM_DD_HH_MM_SS)),

    YYYY_MM_DD_HH_MM(DateTimeFormatter.ofPattern(DateFormatPatterns.YYYY_MM_DD_HH_MM)),

    YYYY_MM_DD_HH(DateTimeFormatter.ofPattern(DateFormatPatterns.YYYY_MM_DD_HH)),

    YYYY_MM_DD(DateTimeFormatter.ofPattern(DateFormatPatterns.YYYY_MM_DD)),

    YYYY_MM(DateTimeFormatter.ofPattern(DateFormatPatterns.YYYY_MM)),

    YYYY(DateTimeFormatter.ofPattern(DateFormatPatterns.YYYY));

    private final DateTimeFormatter formatter;

    public String format(TemporalAccessor temporal) {
        return formatter.format(temporal);
    }
}
