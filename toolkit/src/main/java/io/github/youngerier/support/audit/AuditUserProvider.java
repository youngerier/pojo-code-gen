package io.github.youngerier.support.audit;

/**
 * 当前操作人提供者。使用方实现并注册为 Spring Bean 即可接入审计中的 userId
 * （通常从安全上下文 / token 中获取）。
 */
@FunctionalInterface
public interface AuditUserProvider {

    /**
     * @return 当前用户标识，获取不到时返回 null
     */
    String getUserId();
}
