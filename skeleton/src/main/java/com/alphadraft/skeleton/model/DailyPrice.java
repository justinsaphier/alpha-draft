package com.alphadraft.skeleton.model;

import jakarta.persistence.Column;
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
import java.time.LocalDate;

// One closing price for one stock on one trading day, written by EndOfDayPriceJob. The stock is
// referenced by id without a foreign key, so the price history survives if the stock is deleted.
@Entity
@Table(name = "daily_prices",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_prices_stock_date", columnNames = {"stock_id", "trade_date"}))
public class DailyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "stock_id", nullable = false)
    private int stockId;

    // The stock this price belongs to. Read-only; the id above is what is stored. No database
    // foreign key, so removing a stock from the pool keeps its price history.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Stock stock;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "close_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal closePrice;

    // Where the price came from, e.g. "twelvedata".
    @Column(nullable = false)
    private String source;

    // Required by JPA. Application code uses the constructor below.
    protected DailyPrice() {
    }

    public DailyPrice(int stockId, LocalDate tradeDate, BigDecimal closePrice, String source) {
        this.stockId = stockId;
        this.tradeDate = tradeDate;
        this.closePrice = closePrice;
        this.source = source;
    }

    public long getId() {
        return id;
    }

    public Stock getStock() {
        return stock;
    }

    public int getStockId() {
        return stockId;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public String getSource() {
        return source;
    }
}
