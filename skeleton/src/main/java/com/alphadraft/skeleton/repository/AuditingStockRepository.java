package com.alphadraft.skeleton.repository;

import com.alphadraft.skeleton.model.Stock;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

// Decorator pattern: implements StockRepository AND wraps one. Every save and delete
// is passed to the wrapped repository unchanged, then recorded in the audit log, so
// neither StockService nor the real storage has to know auditing exists. The log itself is
// a StockAuditLog; in the running app that is the stock_audit_log table, so history
// survives restarts.
public class AuditingStockRepository implements StockRepository {

    private final StockRepository delegate;
    private final Clock clock;
    private final StockAuditLog auditLog;

    public AuditingStockRepository(StockRepository delegate, StockAuditLog auditLog) {
        this(delegate, auditLog, Clock.systemUTC());
    }

    AuditingStockRepository(StockRepository delegate, StockAuditLog auditLog, Clock clock) {
        this.delegate = delegate;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public List<Stock> findAll() {
        return delegate.findAll();
    }

    @Override
    public Optional<Stock> findById(int id) {
        return delegate.findById(id);
    }

    @Override
    public boolean existsByTicker(String ticker, Integer excludingId) {
        return delegate.existsByTicker(ticker, excludingId);
    }

    @Override
    public Stock save(Stock stock) {
        String action = delegate.findById(stock.getId()).isPresent() ? "UPDATE" : "CREATE";
        Stock saved = delegate.save(stock);
        record(action, saved.getId(), saved.getTicker());
        return saved;
    }

    @Override
    public boolean deleteById(int id) {
        Optional<Stock> existing = delegate.findById(id);
        boolean deleted = delegate.deleteById(id);
        if (deleted) {
            record("DELETE", id, existing.map(Stock::getTicker).orElse(null));
        }
        return deleted;
    }

    // Oldest change first; a copy, so callers can't edit the log.
    public List<StockAuditEntry> getAuditLog() {
        return List.copyOf(auditLog.findAll());
    }

    private void record(String action, int stockId, String ticker) {
        auditLog.add(new StockAuditEntry(action, stockId, ticker, clock.instant()));
    }
}
