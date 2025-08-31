package com.abc.web.support.audit.example.model;

import com.abc.web.support.audit.annotations.SensitiveField;
import com.abc.web.support.audit.annotations.SensitiveParam.MaskStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * 用户档案信息
 * 演示嵌套对象内部的敏感字段处理
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    
    /**
     * 真实姓名 - 敏感信息，使用默认脱敏
     */
    @SensitiveField(strategy = MaskStrategy.DEFAULT, description = "真实姓名")
    private String realName;
    
    /**
     * 身份证号 - 敏感信息，使用身份证脱敏策略
     */
    @SensitiveField(strategy = MaskStrategy.ID_CARD, description = "身份证号")
    private String idCard;
    
    /**
     * 银行卡号 - 敏感信息，使用银行卡脱敏策略
     */
    @SensitiveField(strategy = MaskStrategy.BANK_CARD, description = "银行卡号")
    private String bankCardNumber;
    
    /**
     * 出生日期 - 不太敏感，正常记录
     */
    private LocalDate birthDate;
    
    /**
     * 性别 - 不敏感
     */
    private String gender;
    
    /**
     * 个人简介 - 使用自定义脱敏策略
     */
    @SensitiveField(
        strategy = MaskStrategy.CUSTOM,
        customExpression = "#value.length() > 20 ? #value.substring(0, 5) + '***[简介已脱敏]***' + #value.substring(#value.length()-5) : '***[简介已脱敏]***'",
        description = "个人简介"
    )
    private String biography;
    
    /**
     * 地址信息 - 嵌套对象
     */
    @SensitiveField(enableNested = true, description = "地址信息")
    private Address address;
}