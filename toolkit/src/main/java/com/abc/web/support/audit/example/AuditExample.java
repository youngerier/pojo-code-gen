package com.abc.web.support.audit.example;

import com.abc.web.support.audit.*;
import com.abc.web.support.audit.annotations.Auditable;
import com.abc.web.support.audit.annotations.IgnoreParam;
import com.abc.web.support.audit.annotations.SensitiveParam;
import com.abc.web.support.audit.annotations.SensitiveParam.MaskStrategy;
import com.abc.web.support.audit.example.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

/**
 * 审计功能使用示例
 * 基于BeanPostProcessor的统一实现方案
 * 
 * @author toolkit
 * @since 1.0.0
 */
@Slf4j
@Service
public class AuditExample {
    
    /**
     * 用户登录示例
     * 演示基本的审计功能和参数注解
     */
    @Auditable(
        operation = "用户登录",
        description = "用户登录验证",
        eventType = AuditEventType.LOGIN,
        includeParameters = true
    )
    public boolean login(String username, 
                        @SensitiveParam(strategy = MaskStrategy.FULL, description = "用户密码") 
                        String password) {
        log.info("用户登录: {}", username);
        // 模拟登录逻辑
        return "admin".equals(username) && "123456".equals(password);
    }
    
    /**
     * 用户注册示例
     * 演示多种脱敏策略
     */
    @Auditable(
        operation = "用户注册",
        description = "新用户注册",
        eventType = AuditEventType.CREATE,
        includeParameters = true
    )
    public void registerUser(String username,
                           @SensitiveParam(strategy = MaskStrategy.FULL)
                           String password,
                           @SensitiveParam(strategy = MaskStrategy.EMAIL) 
                           String email,
                           @SensitiveParam(strategy = MaskStrategy.PHONE) 
                           String phone,
                           @IgnoreParam(reason = "请求对象过大，不记录")
                           Object request) {
        log.info("注册用户: {}", username);
        // 模拟注册逻辑
    }
    
    /**
     * 条件审计示例
     * 只有当参数满足条件时才记录审计
     */
    @Auditable(
        operation = "条件审计",
        description = "演示条件审计功能",
        eventType = AuditEventType.BUSINESS_OPERATION,
        condition = "#important == true",
        includeParameters = true
    )
    public void conditionalAudit(String data, boolean important) {
        log.info("处理数据: {}, 重要: {}", data, important);
        // 只有important为true时才会记录审计
    }
    
    /**
     * 业务标识示例
     * 使用SpEL表达式提取业务标识
     */
    @Auditable(
        operation = "更新用户信息",
        description = "用户信息更新",
        eventType = AuditEventType.UPDATE,
        businessKey = "#userId",
        includeParameters = true,
        includeResult = true
    )
    public String updateUserInfo(String userId, String newInfo) {
        log.info("更新用户 {} 的信息", userId);
        return "更新成功";
    }
    
    /**
     * 异步审计示例
     * 使用异步方式记录审计，不影响主业务性能
     */
    @Auditable(
        operation = "批量处理",
        description = "大批量数据处理",
        eventType = AuditEventType.BUSINESS_OPERATION,
        async = true,
        includeParameters = false  // 大批量数据不记录参数
    )
    public void batchProcess(java.util.List<String> dataList) {
        log.info("批量处理 {} 条数据", dataList.size());
        // 模拟批量处理逻辑
    }
    
    /**
     * 错误处理示例
     * 演示异常情况下的审计记录
     */
    @Auditable(
        operation = "可能失败的操作",
        description = "演示异常审计",
        eventType = AuditEventType.BUSINESS_OPERATION,
        includeException = true
    )
    public void riskyOperation(boolean shouldFail) {
        log.info("执行可能失败的操作");
        if (shouldFail) {
            throw new RuntimeException("模拟业务异常");
        }
    }
    
    /**
     * 新旧配置兼容示例
     * 演示新配置方式的优势
     */
    @Auditable(
        operation = "新配置方式",
        description = "推荐的配置方式示例",
        eventType = AuditEventType.BUSINESS_OPERATION,
        enableParamAnnotations = true,
        sensitiveParamNames = {"legacyParam"}  // 参数名称方式
    )
    public void newConfigurationWay(
            String normal,
            @SensitiveParam(strategy = MaskStrategy.EMAIL, description = "邮箱地址") String email,  // 推荐：通过注解配置
            @IgnoreParam(reason = "大对象，不记录") Object request,        // 推荐：通过注解忽略
            String legacyParam) {   // 通过参数名配置为敏感
        log.info("新配置方式示例执行");
    }
    
    // ================= 复杂对象脱敏功能演示 =================
    
    /**
     * 复杂对象自动脱敏示例
     * 演示使用@SensitiveField注解进行自动嵌套脱敏
     */
    @Auditable(
        operation = "用户注册_复杂对象",
        description = "演示复杂对象内部属性自动脱敏",
        eventType = AuditEventType.CREATE,
        includeParameters = true,
        includeResult = false
    )
    public String registerUserWithComplexObject(
            @SensitiveParam(
                autoNested = true,  // 启用自动嵌套脱敏
                description = "用户注册请求对象"
            ) UserRegisterRequest request) {
        
        log.info("处理用户注册请求: {}", request.getUsername());
        
        // 模拟注册逻辑
        return "用户注册成功，用户ID: " + System.currentTimeMillis();
    }
    
    /**
     * 指定字段路径脱敏示例
     * 演示通过fieldPaths精确控制需要脱敏的字段
     */
    @Auditable(
        operation = "更新用户档案",
        description = "演示指定字段路径脱敏",
        eventType = AuditEventType.UPDATE,
        includeParameters = true
    )
    public void updateUserProfile(
            String userId,
            @SensitiveParam(
                fieldPaths = {
                    "profile.realName",      // 档案中的真实姓名
                    "profile.idCard",        // 档案中的身份证号
                    "profile.address.detailAddress",  // 嵌套对象中的详细地址
                    "emergencyContact.contactPhone"   // 紧急联系人电话
                },
                strategy = MaskStrategy.DEFAULT,
                description = "用户更新请求"
            ) UserRegisterRequest updateRequest) {
        
        log.info("更新用户档案: {}", userId);
        // 模拟更新逻辑
    }
    
    /**
     * 混合脱敏策略示例
     * 演示在同一个复杂对象中使用多种脱敏方式
     */
    @Auditable(
        operation = "实名认证",
        description = "演示混合脱敏策略",
        eventType = AuditEventType.BUSINESS_OPERATION,
        includeParameters = true
    )
    public boolean performKYCVerification(
            @SensitiveParam(
                fieldPaths = {"profile.bankCardNumber"},  // 只脱敏银行卡号
                strategy = MaskStrategy.BANK_CARD,
                autoNested = false,  // 禁用自动脱敏，只处理指定字段
                description = "KYC验证请求"
            ) UserRegisterRequest kycRequest,
            
            @SensitiveParam(strategy = MaskStrategy.FULL, description = "验证密码") 
            String verificationPassword) {
        
        log.info("执行实名认证: {}", kycRequest.getUsername());
        
        // 模拟认证逻辑
        return true;
    }
    
    /**
     * 创建完整演示数据的辅助方法
     * 用于测试复杂对象脱敏功能
     */
    public UserRegisterRequest createSampleUserRequest() {
        // 创建地址信息
        Address address = new Address(
            "广东省",
            "深圳市", 
            "南山区",
            "科技园南区深南大道9999号科技大厦A座1001室",  // 敏感：详细地址
            "518057",
            113.934782,  // 敏感：经度
            22.547234    // 敏感：纬度
        );
        
        // 创建用户档案
        UserProfile profile = new UserProfile(
            "张三",                    // 敏感：真实姓名
            "440301199001011234",      // 敏感：身份证号
            "6225881234567890123",     // 敏感：银行卡号
            LocalDate.of(1990, 1, 1),
            "男",
            "我是一名热爱技术的软件工程师，专注于Java后端开发，有5年以上的企业级项目经验。",  // 敏感：个人简介
            address
        );
        
        // 创建紧急联系人
        EmergencyContact emergencyContact = new EmergencyContact(
            "李四",                     // 敏感：联系人姓名
            "13987654321",             // 敏感：联系人手机号
            "父亲",
            "lisi@example.com",        // 敏感：联系人邮箱
            "工作日18:00后联系"          // 敏感：备注信息
        );
        
        // 创建用户注册请求
        return new UserRegisterRequest(
            "zhangsan123",             // 普通：用户名
            "MySecretPassword123!",    // 敏感：密码
            "zhangsan@example.com",    // 敏感：邮箱
            "13712345678",             // 敏感：手机号
            profile,                    // 敏感：用户档案（嵌套对象）
            emergencyContact,           // 敏感：紧急联系人（嵌套对象）
            "123456",                  // 普通：验证码
            true                        // 普通：同意条款
        );
    }
}

/**
 * 类级别审计示例
 * 整个类的所有公共方法都会被审计
 */
@Service
@Auditable(
    operation = "类级别审计",
    description = "整个服务类的审计",
    eventType = AuditEventType.BUSINESS_OPERATION,
    module = "用户服务"
)
@Slf4j
class ClassLevelAuditExample {
    
    public void method1(String param) {
        log.info("执行方法1: {}", param);
    }
    
    public void method2(String param) {
        log.info("执行方法2: {}", param);
    }
    
    /**
     * 方法级别的注解会覆盖类级别的配置
     */
    @Auditable(
        operation = "特殊方法",
        description = "覆盖类级别配置",
        eventType = AuditEventType.UPDATE
    )
    public void specialMethod(String param) {
        log.info("执行特殊方法: {}", param);
    }
}