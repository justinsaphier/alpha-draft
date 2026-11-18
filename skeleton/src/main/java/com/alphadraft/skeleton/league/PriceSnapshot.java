package com.alphadraft.skeleton.league;

import jakarta.persistence.Column;
import com.alphadraft.skeleton.model.Stock;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.FetchType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

// The month-end price of a drafted stock. monthIndex -1 is the season's starting price.
// apiClose is what the price feed reported; manualClose is an admin correction and wins over it.
@Entity
@Table(name = "league_price_snapshots", uniqueConstraints = @UniqueConstraint(columnNames = {"stockId", "monthIndex"}))
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private int stockId;

    // The stock this row refers to. Read-only; the id above is what is stored. No database foreign key,
    // so deleting a stock from the pool keeps the price history.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stockId", insertable = false, updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Stock stock;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private int monthIndex;

    @Column(precision = 19, scale = 4)
    private BigDecimal apiClose;

    @Column(precision = 19, scale = 4)
    private BigDecimal manualClose;

    protected PriceSnapshot() {
    }

    public PriceSnapshot(int stockId, String ticker, int monthIndex, BigDecimal apiClose) {
        this.stockId = stockId;
        this.ticker = ticker;
        this.monthIndex = monthIndex;
        this.apiClose = apiClose;
    }

    // The price used for scoring: the admin's correction if there is one, otherwise the feed's.
    public BigDecimal effective() {
        return manualClose != null ? manualClose : apiClose;
    }

    public boolean isManual() {
        return manualClose != null;
    }

    public void setApiClose(BigDecimal apiClose) { this.apiClose = apiClose; }
    public void setManualClose(BigDecimal manualClose) { this.manualClose = manualClose; }
    public Stock getStock() { return stock; }
    public int getStockId() { return stockId; }
    public String getTicker() { return ticker; }
    public int getMonthIndex() { return monthIndex; }
    public BigDecimal getApiClose() { return apiClose; }
    public BigDecimal getManualClose() { return manualClose; }
}
