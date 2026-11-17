package com.alphadraft.skeleton.repository;

import java.util.List;

// StockAuditLog stored in the "stock_audit_log" table in Supabase.
public class JpaStockAuditLog implements StockAuditLog {

    private final StockAuditJpaRepository jpa;

    public JpaStockAuditLog(StockAuditJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void add(StockAuditEntry entry) {
        jpa.save(entry);
    }

    @Override
    public List<StockAuditEntry> findAll() {
        return jpa.findAllByOrderByIdAsc();
    }
}
