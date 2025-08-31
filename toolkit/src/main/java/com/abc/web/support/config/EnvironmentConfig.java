package com.abc.web.support.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 环境配置服务
 * 用于判断当前运行环境并控制错误信息的暴露程度
 * 
 * 安全原则：
 * 1. 生产环境：隐藏所有技术细节和堆栈信息，只返回用户友好的错误消息
 * 2. 测试环境：保留部分技术信息，便于测试人员定位问题
 * 3. 开发环境：保留完整的错误详情和堆栈信息，便于开发调试
 * 
 * 配置方式：
 * - 通过 spring.profiles.active 配置环境
 * - 通过 exception.detail.enabled 控制详情暴露
 * - 通过 exception.stacktrace.enabled 控制堆栈暴露
 * 
 * 企业安全要求：
 * - 防止敏感信息泄露
 * - 遵循最小信息暴露原则
 * - 支持多环境差异化配置
 */
@Slf4j
@Component
public class EnvironmentConfig {

    /**
     * 当前激活的环境配置
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 是否启用异常详情暴露（可通过配置覆盖）
     */
    @Value("${exception.detail.enabled:#{null}}")
    private Boolean detailEnabledOverride;

    /**
     * 是否启用堆栈信息暴露（可通过配置覆盖）
     */
    @Value("${exception.stacktrace.enabled:#{null}}")
    private Boolean stacktraceEnabledOverride;

    /**
     * 生产环境标识列表
     */
    private static final List<String> PRODUCTION_PROFILES = Arrays.asList(
            "prod", "production", "prd"
    );

    /**
     * 测试环境标识列表
     */
    private static final List<String> TEST_PROFILES = Arrays.asList(
            "test", "testing", "sit", "uat", "pre"
    );

    /**
     * 开发环境标识列表
     */
    private static final List<String> DEVELOPMENT_PROFILES = Arrays.asList(
            "dev", "development", "local"
    );

    /**
     * 判断是否为生产环境
     */
    public boolean isProductionEnvironment() {
        return PRODUCTION_PROFILES.stream()
                .anyMatch(profile -> activeProfile.toLowerCase().contains(profile));
    }

    /**
     * 判断是否为测试环境
     */
    public boolean isTestEnvironment() {
        return TEST_PROFILES.stream()
                .anyMatch(profile -> activeProfile.toLowerCase().contains(profile));
    }

    /**
     * 判断是否为开发环境
     */
    public boolean isDevelopmentEnvironment() {
        return DEVELOPMENT_PROFILES.stream()
                .anyMatch(profile -> activeProfile.toLowerCase().contains(profile));
    }

    /**
     * 是否应该暴露异常详情
     * 
     * 决策逻辑：
     * 1. 如果有明确的配置覆盖，使用配置值
     * 2. 生产环境：不暴露详情
     * 3. 测试环境：暴露基本详情
     * 4. 开发环境：暴露完整详情
     */
    public boolean shouldExposeExceptionDetails() {
        if (detailEnabledOverride != null) {
            return detailEnabledOverride;
        }
        
        // 生产环境不暴露详情
        if (isProductionEnvironment()) {
            return false;
        }
        
        // 测试和开发环境暴露详情
        return isTestEnvironment() || isDevelopmentEnvironment();
    }

    /**
     * 是否应该暴露堆栈信息
     * 
     * 决策逻辑：
     * 1. 如果有明确的配置覆盖，使用配置值
     * 2. 生产环境：绝对不暴露堆栈
     * 3. 测试环境：不暴露堆栈（避免日志过多）
     * 4. 开发环境：暴露堆栈信息
     */
    public boolean shouldExposeStackTrace() {
        if (stacktraceEnabledOverride != null) {
            return stacktraceEnabledOverride;
        }
        
        // 只有开发环境才暴露堆栈信息
        return isDevelopmentEnvironment();
    }

    /**
     * 获取当前环境类型
     */
    public EnvironmentType getCurrentEnvironmentType() {
        if (isProductionEnvironment()) {
            return EnvironmentType.PRODUCTION;
        } else if (isTestEnvironment()) {
            return EnvironmentType.TEST;
        } else {
            return EnvironmentType.DEVELOPMENT;
        }
    }

    /**
     * 获取环境友好的显示名称
     */
    public String getEnvironmentDisplayName() {
        return switch (getCurrentEnvironmentType()) {
            case PRODUCTION -> "生产环境";
            case TEST -> "测试环境";
            case DEVELOPMENT -> "开发环境";
        };
    }

    /**
     * 环境类型枚举
     */
    public enum EnvironmentType {
        PRODUCTION,    // 生产环境
        TEST,          // 测试环境  
        DEVELOPMENT    // 开发环境
    }

    /**
     * 组件初始化时输出环境信息
     */
    public void logEnvironmentInfo() {
        log.info("当前运行环境: {} ({})", getEnvironmentDisplayName(), activeProfile);
        log.info("异常详情暴露: {}", shouldExposeExceptionDetails() ? "启用" : "禁用");
        log.info("堆栈信息暴露: {}", shouldExposeStackTrace() ? "启用" : "禁用");
        
        if (isProductionEnvironment()) {
            log.warn("⚠️  生产环境已启用安全模式，异常详情和堆栈信息将被隐藏");
        }
    }
}