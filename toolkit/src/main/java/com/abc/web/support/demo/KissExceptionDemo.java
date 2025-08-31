package com.abc.web.support.demo;

import com.abc.web.support.ExceptionUtils;
import com.abc.web.support.enums.I18nCommonExceptionCode;
import com.abc.web.support.exception.ExceptionHandlerResult;

/**
 * KISS原则异常体系使用演示
 * 展示简化后的API使用方式
 * 
 * 设计目标：
 * - 让开发者看到简化前后的对比
 * - 展示统一的使用体验
 * - 体现KISS原则的价值
 */
public class KissExceptionDemo {

    /**
     * 演示：异常抛出的简化使用
     */
    public void demonstrateExceptionThrowing() {
        // ✅ KISS原则：直接使用，无需手动处理国际化
        ExceptionUtils.throwBusiness(I18nCommonExceptionCode.USER_NOT_FOUND);
        
        // ✅ KISS原则：带参数也很简洁  
        ExceptionUtils.throwBusiness(I18nCommonExceptionCode.PARAM_OUT_OF_RANGE, "age", "18", "65");
    }

    /**
     * 演示：异常策略中的简化使用
     */
    public ExceptionHandlerResult demonstrateExceptionStrategy() {
        // ❌ 简化前：需要手动处理国际化（繁琐）
        // String message = ExceptionUtils.getLocalizedMessage(I18nCommonExceptionCode.DATABASE_ERROR);
        // return ExceptionHandlerResult.system("2000", message);
        
        // ✅ 简化后：一行代码搞定，自动国际化
        return ExceptionHandlerResult.system(I18nCommonExceptionCode.DATABASE_ERROR);
    }

    /**
     * 演示：带参数的异常处理简化
     */
    public ExceptionHandlerResult demonstrateParameterizedExceptions() {
        // ❌ 简化前：手动处理参数和国际化（复杂）
        // String message = ExceptionUtils.getLocalizedMessage(
        //     I18nCommonExceptionCode.PARAM_OUT_OF_RANGE, "age", "18", "65"
        // );
        // return ExceptionHandlerResult.validation("1102", message);
        
        // ✅ 简化后：直接传参数，内部自动处理
        return ExceptionHandlerResult.validation(
            I18nCommonExceptionCode.PARAM_OUT_OF_RANGE, "age", "18", "65"
        );
    }

    /**
     * 演示：不同类型异常的统一API
     */
    public void demonstrateUnifiedAPI() {
        // 所有类型的异常都使用相同的简化API风格
        
        // 业务异常
        ExceptionHandlerResult business = ExceptionHandlerResult.business(
            I18nCommonExceptionCode.DATA_NOT_FOUND
        );
        
        // 系统异常  
        ExceptionHandlerResult system = ExceptionHandlerResult.system(
            I18nCommonExceptionCode.DATABASE_ERROR
        );
        
        // 验证异常
        ExceptionHandlerResult validation = ExceptionHandlerResult.validation(
            I18nCommonExceptionCode.VALIDATION_ERROR
        );
        
        // 认证异常
        ExceptionHandlerResult auth = ExceptionHandlerResult.authentication(
            I18nCommonExceptionCode.UNAUTHORIZED  
        );
        
        // 授权异常
        ExceptionHandlerResult authz = ExceptionHandlerResult.authorization(
            I18nCommonExceptionCode.PERMISSION_DENIED
        );
        
        System.out.println("🎉 所有异常类型都使用统一的简化API！");
    }

    /**
     * KISS原则的价值体现
     */
    public void demonstrateKissValue() {
        System.out.println("📈 KISS原则优化效果:");
        System.out.println("✅ 代码行数：从3行减少到1行");
        System.out.println("✅ 学习成本：新手5分钟上手"); 
        System.out.println("✅ 维护成本：统一API，无需记忆多种写法");
        System.out.println("✅ 错误率：自动处理，减少人为错误");
        System.out.println("✅ 开发效率：专注业务逻辑，不用操心技术细节");
        
        System.out.println("\n🎯 设计理念：Keep It Simple and Stupid！");
        System.out.println("   让异常处理变得如此简单，连实习生都能快速上手！");
    }
}