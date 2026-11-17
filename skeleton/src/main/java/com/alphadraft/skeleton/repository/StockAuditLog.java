package com.alphadraft.skeleton.repository;

import java.util.List;

// Where AuditingStockRepository writes its entries. The app uses the database-backed
// JpaStockAuditLog; tests can pass a simple list instead.
public interface StockAuditLog {

    void add(StockAuditEntry entry);

    // Oldest change first.
    List<StockAuditEntry> findAll();
}
