package com.abc.web.support.audit.example;

import com.abc.web.support.audit.*;
import com.abc.web.support.audit.annotations.Auditable;
import com.abc.web.support.audit.annotations.IgnoreParam;
import com.abc.web.support.audit.annotations.SensitiveParam;
import com.abc.web.support.audit.annotations.SensitiveParam.MaskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
     * 混合配置示例
     * 演示新旧配置方式的兼容性
     */
    @Auditable(
        operation = "混合配置",
        description = "新旧配置兼容示例",
        eventType = AuditEventType.BUSINESS_OPERATION,
        enableParamAnnotations = true,
        sensitiveParams = {1},  // 传统索引方式
        sensitiveParamNames = {"legacyParam"}  // 参数名方式
    )
    public void mixedConfiguration(
            String normal,
            String indexSensitive,  // 通过索引配置为敏感
            @SensitiveParam(strategy = MaskStrategy.EMAIL) String annotationSensitive,  // 通过注解配置
            String legacyParam) {   // 通过参数名配置为敏感
        log.info("混合配置示例执行");
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