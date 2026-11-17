package com.alphadraft.skeleton.testsupport;

import com.alphadraft.skeleton.repository.StockAuditEntry;
import com.alphadraft.skeleton.repository.StockAuditLog;
import java.util.ArrayList;
import java.util.List;

// Test stand-in for the database-backed audit log.
public class InMemoryStockAuditLog implements StockAuditLog {

    private final List<StockAuditEntry> entries = new ArrayList<>();

    @Override
    public void add(StockAuditEntry entry) {
        entries.add(entry);
    }

    @Override
    public List<StockAuditEntry> findAll() {
        return List.copyOf(entries);
    }
}
