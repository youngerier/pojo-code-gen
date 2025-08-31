package com.abc.web.support.audit.example.model;

import com.abc.web.support.audit.annotations.SensitiveField;
import com.abc.web.support.audit.annotations.SensitiveParam.MaskStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地址信息
 * 演示多层嵌套对象的脱敏处理
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    
    /**
     * 省份 - 不敏感信息
     */
    private String province;
    
    /**
     * 城市 - 不敏感信息
     */
    private String city;
    
    /**
     * 区县 - 不敏感信息
     */
    private String district;
    
    /**
     * 详细地址 - 敏感信息，包含具体门牌号
     */
    @SensitiveField(strategy = MaskStrategy.DEFAULT, description = "详细地址")
    private String detailAddress;
    
    /**
     * 邮政编码 - 不敏感
     */
    private String zipCode;
    
    /**
     * 经度 - 位置敏感信息
     */
    @SensitiveField(
        strategy = MaskStrategy.CUSTOM,
        customExpression = "#value.toString().substring(0, #value.toString().indexOf('.') + 3) + '***'",
        description = "经度坐标"
    )
    private Double longitude;
    
    /**
     * 纬度 - 位置敏感信息
     */
    @SensitiveField(
        strategy = MaskStrategy.CUSTOM,
        customExpression = "#value.toString().substring(0, #value.toString().indexOf('.') + 3) + '***'",
        description = "纬度坐标"
    )
    private Double latitude;
}