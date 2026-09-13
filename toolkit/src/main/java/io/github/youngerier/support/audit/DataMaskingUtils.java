package io.github.youngerier.support.audit;

import io.github.youngerier.support.audit.annotations.SensitiveParam.MaskStrategy;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 数据脱敏工具，提供固定脱敏策略与 SpEL 自定义策略。
 */
public final class DataMaskingUtils {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^\\d{13,19}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");

    private DataMaskingUtils() {
    }

    public static Object mask(Object data, MaskStrategy strategy, String customExpression) {
        if (data == null) {
            return null;
        }
        String str = data.toString();
        if (!StringUtils.hasText(str)) {
            return data;
        }
        return switch (strategy) {
            case FULL -> maskFull();
            case EMAIL -> maskEmail(str);
            case PHONE -> maskPhone(str);
            case BANK_CARD -> maskBankCard(str);
            case ID_CARD -> maskIdCard(str);
            case CUSTOM -> maskCustom(data, customExpression);
            default -> maskDefault(str);
        };
    }

    /** 保留前2后2 */
    private static String maskDefault(String str) {
        if (str.length() <= 4) {
            return "****";
        }
        return str.substring(0, 2) + "****" + str.substring(str.length() - 2);
    }

    private static String maskFull() {
        return "****";
    }

    private static String maskEmail(String str) {
        if (!EMAIL_PATTERN.matcher(str).matches()) {
            return maskDefault(str);
        }
        int atIndex = str.indexOf('@');
        if (atIndex <= 0) {
            return maskDefault(str);
        }
        return str.charAt(0) + "***" + str.substring(atIndex);
    }

    private static String maskPhone(String str) {
        if (!PHONE_PATTERN.matcher(str).matches()) {
            return maskDefault(str);
        }
        return str.substring(0, 3) + "****" + str.substring(7);
    }

    private static String maskBankCard(String str) {
        if (!BANK_CARD_PATTERN.matcher(str).matches() || str.length() <= 4) {
            return maskDefault(str);
        }
        return "**** **** **** " + str.substring(str.length() - 4);
    }

    private static String maskIdCard(String str) {
        if (!ID_CARD_PATTERN.matcher(str).matches()) {
            return maskDefault(str);
        }
        return str.substring(0, 4) + "**********" + str.substring(14);
    }

    private static Object maskCustom(Object data, String customExpression) {
        if (!StringUtils.hasText(customExpression)) {
            return maskDefault(data.toString());
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("value", data);
            return PARSER.parseExpression(customExpression).getValue(context);
        } catch (Exception e) {
            return maskDefault(data.toString());
        }
    }
}
