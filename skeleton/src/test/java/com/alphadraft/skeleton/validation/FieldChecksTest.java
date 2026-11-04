package com.alphadraft.skeleton.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldChecksTest {

    @Test
    void nullIsBlank() {
        assertTrue(FieldChecks.isBlank(null));
    }

    @Test
    void emptyAndWhitespaceAreBlank() {
        assertTrue(FieldChecks.isBlank(""));
        assertTrue(FieldChecks.isBlank("   "));
    }

    @Test
    void textIsNotBlank() {
        assertFalse(FieldChecks.isBlank("AAPL"));
        assertFalse(FieldChecks.isBlank(" a "));
    }
}
