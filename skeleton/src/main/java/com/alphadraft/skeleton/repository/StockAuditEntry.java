package com.alphadraft.skeleton.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

// One recorded change to the stock pool: CREATE, UPDATE or DELETE. Stored in the
// "stock_audit_log" table so the history survives restarts. stockId is a plain column, not a
// relationship, because the log must keep its DELETE entries after the stock row is gone.
@Entity
@Table(name = "stock_audit_log")
public class StockAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String action;

    @Column(name = "stock_id", nullable = false)
    private int stockId;

    private String ticker;

    // "timestamp" is a reserved word in some databases, so the column has its own name.
    @Column(name = "changed_at", nullable = false)
    private Instant timestamp;

    // Required by JPA. Application code uses the constructor below.
    protected StockAuditEntry() {
    }

    public StockAuditEntry(String action, int stockId, String ticker, Instant timestamp) {
        this.action = action;
        this.stockId = stockId;
        this.ticker = ticker;
        this.timestamp = timestamp;
    }

    public String action() {
        return action;
    }

    public int stockId() {
        return stockId;
    }

    public String ticker() {
        return ticker;
    }

    public Instant timestamp() {
        return timestamp;
    }

    // Two entries are equal when they describe the same change; the database id is ignored,
    // so an entry read back from the table equals the one that was written.
    @Override
    public boolean equals(Object other) {
        return other instanceof StockAuditEntry entry
                && stockId == entry.stockId
                && Objects.equals(action, entry.action)
                && Objects.equals(ticker, entry.ticker)
                && Objects.equals(timestamp, entry.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, stockId, ticker, timestamp);
    }

    @Override
    public String toString() {
        return "StockAuditEntry[" + action + " " + stockId + " " + ticker + " at " + timestamp + "]";
    }
}
