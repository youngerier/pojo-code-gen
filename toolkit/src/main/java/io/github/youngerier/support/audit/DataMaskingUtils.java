package io.github.youngerier.support.audit;

import io.github.youngerier.support.audit.annotations.SensitiveParam;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 数据脱敏工具类
 * 提供各种数据脱敏策略的实现
 * 
 * @author toolkit
 * @since 1.0.0
 */
public class DataMaskingUtils {
    
    private static final ExpressionParser parser = new SpelExpressionParser();
    
    // 邮箱正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    
    // 手机号正则（简单版本，支持11位数字）
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    // 银行卡号正则（13-19位数字）
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^\\d{13,19}$");
    
    // 身份证号正则（18位）
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");
    
    /**
     * 根据策略对数据进行脱敏
     */
    public static Object maskData(Object data, SensitiveParam.MaskStrategy strategy, String customExpression) {
        if (data == null) {
            return null;
        }
        
        String str = data.toString();
        if (!StringUtils.hasText(str)) {
            return data;
        }
        
        switch (strategy) {
            case DEFAULT:
                return maskDefault(str);
            case FULL:
                return maskFull(str);
            case EMAIL:
                return maskEmail(str);
            case PHONE:
                return maskPhone(str);
            case BANK_CARD:
                return maskBankCard(str);
            case ID_CARD:
                return maskIdCard(str);
            case CUSTOM:
                return maskCustom(data, customExpression);
            default:
                return maskDefault(str);
        }
    }
    
    /**
     * 默认脱敏：保留前2位和后2位，中间用****代替
     */
    private static String maskDefault(String str) {
        if (str.length() <= 4) {
            return "****";
        }
        return str.substring(0, 2) + "****" + str.substring(str.length() - 2);
    }
    
    /**
     * 全部脱敏：所有字符都用****代替
     */
    private static String maskFull(String str) {
        return "****";
    }
    
    /**
     * 邮箱脱敏：保留@前1位和@后的域名
     */
    private static String maskEmail(String str) {
        if (!EMAIL_PATTERN.matcher(str).matches()) {
            return maskDefault(str);
        }
        
        int atIndex = str.indexOf('@');
        if (atIndex <= 0) {
            return maskDefault(str);
        }
        
        String localPart = str.substring(0, atIndex);
        String domainPart = str.substring(atIndex);
        
        if (localPart.length() == 1) {
            return "*" + domainPart;
        }
        
        return localPart.charAt(0) + "***" + domainPart;
    }
    
    /**
     * 手机号脱敏：保留前3位和后4位
     */
    private static String maskPhone(String str) {
        if (!PHONE_PATTERN.matcher(str).matches()) {
            return maskDefault(str);
        }
        
        return str.substring(0, 3) + "****" + str.substring(7);
    }
    
    /**
     * 银行卡脱敏：保留后4位
     */
    private static String maskBankCard(String str) {
        if (!BANK_CARD_PATTERN.matcher(str).matches()) {
            return maskDefault(str);
        }
        
        if (str.length() <= 4) {
            return "****";
        }
        
        return "**** **** **** " + str.substring(str.length() - 4);
    }
    
    /**
     * 身份证脱敏：保留前4位和后4位
     */
    private static String maskIdCard(String str) {
        if (!ID_CARD_PATTERN.matcher(str).matches()) {
            return maskDefault(str);
        }
        
        return str.substring(0, 4) + "**********" + str.substring(14);
    }
    
    /**
     * 自定义脱敏：使用SpEL表达式
     */
    private static Object maskCustom(Object data, String customExpression) {
        if (!StringUtils.hasText(customExpression)) {
            return maskDefault(data.toString());
        }
        
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("value", data);
            
            return parser.parseExpression(customExpression).getValue(context);
        } catch (Exception e) {
            // 自定义表达式执行失败时，使用默认脱敏策略
            return maskDefault(data.toString());
        }
    }
    
    /**
     * 智能脱敏：根据数据内容自动判断脱敏策略
     */
    public static String smartMask(String str) {
        if (!StringUtils.hasText(str)) {
            return str;
        }
        
        // 判断是否为邮箱
        if (EMAIL_PATTERN.matcher(str).matches()) {
            return maskEmail(str);
        }
        
        // 判断是否为手机号
        if (PHONE_PATTERN.matcher(str).matches()) {
            return maskPhone(str);
        }
        
        // 判断是否为银行卡号
        if (BANK_CARD_PATTERN.matcher(str).matches()) {
            return maskBankCard(str);
        }
        
        // 判断是否为身份证号
        if (ID_CARD_PATTERN.matcher(str).matches()) {
            return maskIdCard(str);
        }
        
        // 默认脱敏
        return maskDefault(str);
    }
}