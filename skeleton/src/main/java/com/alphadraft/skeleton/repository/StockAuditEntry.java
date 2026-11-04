package com.alphadraft.skeleton.repository;

import java.time.Instant;

// One recorded change to the stock pool: CREATE, UPDATE or DELETE.
public record StockAuditEntry(String action, int stockId, String ticker, Instant timestamp) {
}
