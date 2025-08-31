package com.abc.web.support.audit.example.model;

import com.abc.web.support.audit.annotations.SensitiveField;
import com.abc.web.support.audit.annotations.SensitiveParam.MaskStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 紧急联系人信息
 * 演示复杂对象脱敏的另一个嵌套层级
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {
    
    /**
     * 联系人姓名 - 敏感信息
     */
    @SensitiveField(strategy = MaskStrategy.DEFAULT, description = "联系人姓名")
    private String contactName;
    
    /**
     * 联系人手机号 - 敏感信息
     */
    @SensitiveField(strategy = MaskStrategy.PHONE, description = "联系人手机号")
    private String contactPhone;
    
    /**
     * 与本人关系 - 不敏感信息
     */
    private String relationship;
    
    /**
     * 联系人邮箱 - 敏感信息
     */
    @SensitiveField(strategy = MaskStrategy.EMAIL, description = "联系人邮箱")
    private String contactEmail;
    
    /**
     * 备注信息 - 可能包含敏感内容，使用默认脱敏
     */
    @SensitiveField(strategy = MaskStrategy.DEFAULT, description = "备注信息")
    private String remarks;
}