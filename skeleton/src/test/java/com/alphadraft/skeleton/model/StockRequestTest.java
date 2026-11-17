package com.alphadraft.skeleton.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class StockRequestTest {

    @Test
    void normalizedTrimsAndUppercasesTicker() {
        StockRequest normalized = new StockRequest("  nvda ", "NVIDIA", "Technology").normalized();

        assertEquals("NVDA", normalized.ticker());
    }

    @Test
    void normalizedTrimsNameAndSector() {
        StockRequest normalized = new StockRequest("NVDA", "  NVIDIA Corporation ", " Technology ").normalized();

        assertEquals("NVIDIA Corporation", normalized.name());
        assertEquals("Technology", normalized.sector());
    }

    @Test
    void normalizedKeepsMissingFieldsNull() {
        StockRequest normalized = new StockRequest(null, null, null).normalized();

        assertNull(normalized.ticker());
        assertNull(normalized.name());
        assertNull(normalized.sector());
    }
}
