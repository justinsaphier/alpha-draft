package com.alphadraft.skeleton.repository;

import com.alphadraft.skeleton.model.DailyPrice;
import java.time.LocalDate;
import java.util.List;

// DailyPriceRepository backed by Supabase PostgreSQL through Spring Data JPA.
public class JpaDailyPriceRepository implements DailyPriceRepository {

    private final DailyPriceJpaRepository jpa;

    public JpaDailyPriceRepository(DailyPriceJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean exists(int stockId, LocalDate tradeDate) {
        return jpa.existsByStockIdAndTradeDate(stockId, tradeDate);
    }

    @Override
    public DailyPrice save(DailyPrice price) {
        return jpa.save(price);
    }

    @Override
    public List<DailyPrice> findLatestForEachStock() {
        return jpa.findLatestForEachStock();
    }
}
