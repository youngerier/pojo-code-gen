package io.github.youngerier.support.page;

import io.github.youngerier.support.enums.DefaultOrderField;
import io.github.youngerier.support.enums.QueryOrderType;
import io.github.youngerier.support.exception.BaseException;
import io.github.youngerier.support.page.flex.QueryWrapperHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link QueryWrapperHelper} 的安全防线测试。
 *
 * <p>MyBatis-Flex 的 {@code orderBy(String, boolean)} 会把字符串作为原始 SQL 片段拼进 ORDER BY，
 * 因此这里必须守住「非标识符即拒绝」的底线：一旦消费方用自定义 {@link QueryOrderField}
 * 透传用户输入，库要拦得住。
 */
class QueryWrapperHelperTest {

    /** 模拟「不安全的实现」：字段名来自外部输入 */
    private static final class RawStringOrderField implements QueryOrderField {

        private final String field;

        private RawStringOrderField(String field) {
            this.field = field;
        }

        @Override
        public String getOrderField() {
            return field;
        }
    }

    static class RawQuery extends AbstractPageQuery<RawStringOrderField> {
    }

    static class EnumQuery extends AbstractPageQuery<DefaultOrderField> {
    }

    private static RawQuery rawQuery(String... fields) {
        RawQuery query = new RawQuery();
        RawStringOrderField[] orderFields = new RawStringOrderField[fields.length];
        QueryOrderType[] orderTypes = new QueryOrderType[fields.length];
        for (int i = 0; i < fields.length; i++) {
            orderFields[i] = new RawStringOrderField(fields[i]);
            orderTypes[i] = QueryOrderType.ASC;
        }
        query.setOrderFields(orderFields);
        query.setOrderTypes(orderTypes);
        return query;
    }

    @Test
    void acceptsPlainIdentifierColumn() {
        assertDoesNotThrow(() -> QueryWrapperHelper.withOrder(rawQuery("gmt_create")));
        assertDoesNotThrow(() -> QueryWrapperHelper.withOrder(rawQuery("_private", "col_1")));
    }

    @Test
    void acceptsEnumBackedOrderFields() {
        EnumQuery query = new EnumQuery();
        query.setOrderFields(new DefaultOrderField[]{DefaultOrderField.GMT_CREATE});
        query.setOrderTypes(new QueryOrderType[]{QueryOrderType.DESC});

        assertDoesNotThrow(() -> QueryWrapperHelper.withOrder(query));
    }

    @Test
    void returnsEmptyWrapperWhenNoOrderRequested() {
        assertDoesNotThrow(() -> QueryWrapperHelper.withOrder(new RawQuery()));
    }

    @Test
    void rejectsSqlInjectionAttempts() {
        assertThrows(BaseException.class,
                () -> QueryWrapperHelper.withOrder(rawQuery("id; drop table t_user --")));
        assertThrows(BaseException.class,
                () -> QueryWrapperHelper.withOrder(rawQuery("id) --")));
        assertThrows(BaseException.class,
                () -> QueryWrapperHelper.withOrder(rawQuery("id asc, (select 1)")));
        assertThrows(BaseException.class,
                () -> QueryWrapperHelper.withOrder(rawQuery("1=1")));
        assertThrows(BaseException.class,
                () -> QueryWrapperHelper.withOrder(rawQuery("id`")));
    }

    @Test
    void rejectsNullBlankAndIllegalColumnNames() {
        assertThrows(BaseException.class, () -> QueryWrapperHelper.withOrder(rawQuery((String) null)));
        assertThrows(BaseException.class, () -> QueryWrapperHelper.withOrder(rawQuery("")));
        assertThrows(BaseException.class, () -> QueryWrapperHelper.withOrder(rawQuery("   ")));
        assertThrows(BaseException.class, () -> QueryWrapperHelper.withOrder(rawQuery("gmt create")));
        assertThrows(BaseException.class, () -> QueryWrapperHelper.withOrder(rawQuery("1col")));
    }

    @Test
    void rejectionIsReportedAsBadRequestInsteadOfServerError() {
        BaseException ex = assertThrows(BaseException.class,
                () -> QueryWrapperHelper.withOrder(rawQuery("id; drop table t_user --")));

        // 客户端传入非法参数属于 400，而不是 500
        assertTrue(ex.getCode().httpStatus() < 500, "非法排序字段应按 4xx 返回");
    }
}
