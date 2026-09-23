package io.github.youngerier.support.page.flex;

import com.mybatisflex.core.query.QueryWrapper;
import io.github.youngerier.support.enums.QueryOrderType;
import io.github.youngerier.support.exception.BaseException;
import io.github.youngerier.support.page.AbstractPageQuery;
import io.github.youngerier.support.page.QueryOrderField;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * MyBatis-Flex 查询包装辅助工具。
 *
 * <p>位于 {@code toolkit-mybatis-flex} 模块：只有本类需要 MyBatis-Flex，
 * 分页模型（{@link AbstractPageQuery}、{@code Pagination}）在 {@code toolkit-core} 中，
 * 使用方若只需分页模型与统一响应体，不必引入 MyBatis-Flex。
 *
 * <p><b>安全约定</b>：MyBatis-Flex 的 {@code QueryWrapper.orderBy(String, boolean)} 会把传入的字符串
 * 作为<em>原始</em> SQL 片段拼进 {@code ORDER BY}（内部走 {@code RawQueryColumn}，不做列名转义、
 * 也不走参数绑定）。因此 {@link QueryOrderField#getOrderField()} 的返回值<strong>必须是可信的、
 * 编译期确定的列名</strong>——推荐实现为封闭枚举（如 {@link io.github.youngerier.support.enums.DefaultOrderField}），
 * 严禁直接透传用户输入。
 *
 * <p>本类仍会做一次标识符白名单校验作为兜底防线：只接受
 * {@code [A-Za-z_][A-Za-z0-9_]*} 形式的列名，其他情况抛 400 业务异常。
 **/
public final class QueryWrapperHelper {

    /**
     * 合法列名：字母或下划线开头，后接字母/数字/下划线
     */
    private static final Pattern SAFE_ORDER_FIELD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private QueryWrapperHelper() {
        throw new AssertionError();
    }

    /**
     * 依据 {@link AbstractPageQuery} 构造带排序的 {@link QueryWrapper}。
     *
     * @param query 分页查询参数
     * @return 带 ORDER BY 的 QueryWrapper；无需排序时返回空 QueryWrapper
     * @throws BaseException 排序字段为空或不是合法标识符（HTTP 400）
     */
    public static QueryWrapper withOrder(AbstractPageQuery<?> query) {
        QueryWrapper result = QueryWrapper.create();
        if (!query.requireOrderBy()) {
            return result;
        }
        QueryOrderField[] orderFields = query.getOrderFields();
        QueryOrderType[] orderTypes = query.getOrderTypes();
        for (int i = 0; i < orderFields.length; i++) {
            QueryOrderField orderField = orderFields[i];
            String column = orderField == null ? null : orderField.getOrderField();
            if (column == null || !SAFE_ORDER_FIELD.matcher(column).matches()) {
                // 兜底防线：正常情况下 orderField 来自封闭枚举，走到这里说明实现方透传了不可信输入
                throw BaseException.badRequest("非法的排序字段: {}", column);
            }
            result.orderBy(column, Objects.equals(orderTypes[i], QueryOrderType.ASC));
        }
        return result;
    }
}
