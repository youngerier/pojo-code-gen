package io.github.youngerier.support.audit;

import io.github.youngerier.support.audit.annotations.SensitiveParam.MaskStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DataMaskingUtilsTest {

    @Test
    void masksPhoneKeepingHeadAndTail() {
        assertEquals("138****8888", DataMaskingUtils.mask("13812348888", MaskStrategy.PHONE, null));
    }

    @Test
    void masksEmailKeepingDomain() {
        assertEquals("z***@example.com", DataMaskingUtils.mask("zhang@example.com", MaskStrategy.EMAIL, null));
    }

    @Test
    void fullMaskHidesEverything() {
        assertEquals("****", DataMaskingUtils.mask("anything", MaskStrategy.FULL, null));
    }

    @Test
    void nullPassesThrough() {
        assertNull(DataMaskingUtils.mask(null, MaskStrategy.DEFAULT, null));
    }

    @Test
    void customSpelExpressionIsApplied() {
        Object result = DataMaskingUtils.mask("123456", MaskStrategy.CUSTOM, "#value.substring(0,2) + '****'");
        assertEquals("12****", result);
    }
}
