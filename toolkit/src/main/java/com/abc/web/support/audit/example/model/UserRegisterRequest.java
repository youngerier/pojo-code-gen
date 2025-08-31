package com.abc.web.support.audit.example.model;

import com.abc.web.support.audit.annotations.SensitiveField;
import com.abc.web.support.audit.annotations.SensitiveParam.MaskStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册请求对象
 * 演示复杂对象内部属性脱敏功能
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest {
    
    /**
     * 用户名 - 不敏感，正常记录
     */
    private String username;
    
    /**
     * 密码 - 敏感字段，完全脱敏
     */
    @SensitiveField(strategy = MaskStrategy.FULL, description = "用户密码")
    private String password;
    
    /**
     * 邮箱 - 敏感字段，邮箱脱敏
     */
    @SensitiveField(strategy = MaskStrategy.EMAIL, description = "用户邮箱")
    private String email;
    
    /**
     * 手机号 - 敏感字段，手机号脱敏
     */
    @SensitiveField(strategy = MaskStrategy.PHONE, description = "手机号码")
    private String phone;
    
    /**
     * 用户档案 - 嵌套对象，内部也有敏感字段
     */
    @SensitiveField(enableNested = true, description = "用户档案")
    private UserProfile profile;
    
    /**
     * 紧急联系人 - 嵌套对象
     */
    @SensitiveField(enableNested = true, description = "紧急联系人")
    private EmergencyContact emergencyContact;
    
    /**
     * 验证码 - 不需要脱敏的字段
     */
    private String verificationCode;
    
    /**
     * 同意条款 - 布尔值，不需要脱敏
     */
    private Boolean agreeTerms;
}