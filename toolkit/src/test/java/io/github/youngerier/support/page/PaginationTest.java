package io.github.youngerier.support.page;

import io.github.youngerier.support.enums.DefaultOrderField;
import io.github.youngerier.support.enums.QueryType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationTest {

    static class TestQuery extends AbstractPageQuery<DefaultOrderField> {
    }

    @Test
    void ofCopiesPagingStateFromQuery() {
        TestQuery query = new TestQuery();
        query.setQueryPage(3);
        query.setQuerySize(50);

        Pagination<String> page = Pagination.of(List.of("a", "b"), query, 200);

        assertEquals(200, page.getTotal());
        assertEquals(2, page.getRecords().size());
        assertEquals(3, page.getQueryPage());
        assertEquals(50, page.getQuerySize());
        assertEquals(QueryType.QUERY_BOTH, page.getQueryType());
        assertEquals(4, page.getTotalPages());
        assertTrue(page.hasNext());
        assertTrue(page.hasRecords());
    }

    @Test
    void convertMapsRecordsAndKeepsPaging() {
        TestQuery query = new TestQuery();
        Pagination<Integer> source = Pagination.of(List.of(1, 2, 3), query, 3);

        Pagination<String> converted = Pagination.convert(source, i -> "n" + i);

        assertEquals(List.of("n1", "n2", "n3"), converted.getRecords());
        assertEquals(3, converted.getTotal());
    }

    @Test
    void emptyPageHasNoRecords() {
        Pagination<Object> empty = Pagination.empty(new TestQuery());
        assertEquals(0, empty.getTotal());
        assertTrue(empty.getRecords().isEmpty());
        assertTrue(empty.isEmpty());
        assertFalse(empty.hasNext());
    }

    @Test
    void querySizeOverLimitIsRejected() {
        TestQuery query = new TestQuery();
        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> query.setQuerySize(AbstractPageQuery.MAX_QUERY_SIZE + 1));
        assertTrue(ex.getMessage().contains(String.valueOf(AbstractPageQuery.MAX_QUERY_SIZE)));
    }

    @Test
    void requireOrderByOnlyWhenFieldsAndTypesMatch() {
        TestQuery query = new TestQuery();
        assertFalse(query.requireOrderBy());

        query.setOrderFields(new DefaultOrderField[]{DefaultOrderField.GMT_CREATE});
        query.setOrderTypes(new io.github.youngerier.support.enums.QueryOrderType[]{
                io.github.youngerier.support.enums.QueryOrderType.ASC});
        assertTrue(query.requireOrderBy());
    }
}
