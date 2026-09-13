package io.github.youngerier.support;

import io.github.youngerier.support.exception.DefaultExceptionCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseTest {

    @Test
    void okBuildsSuccessResponse() {
        Response<String> response = Response.ok("data");
        assertEquals(200, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals("data", response.getData());
        assertTrue(response.isOk());
        assertFalse(response.isError());
    }

    @Test
    void errorWithExceptionCodeUsesDescriptionAsMessage() {
        Response<Void> response = Response.error(DefaultExceptionCode.BAD_REQUEST);
        assertEquals(400, response.getCode());
        assertEquals("请求不合法", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void errorWithRawMessageDefaultsTo500() {
        Response<Void> response = Response.error("boom");
        assertEquals(500, response.getCode());
        assertEquals("boom", response.getMessage());
    }
}
