package com.alphadraft.skeleton.repository;

import com.alphadraft.skeleton.model.Stock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;

// StockRepository backed by Supabase PostgreSQL through Spring Data JPA.
public class JpaStockRepository implements StockRepository {

    private final StockJpaRepository jpa;

    public JpaStockRepository(StockJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Stock> findAll() {
        return jpa.findAll(Sort.by("id"));
    }

    @Override
    public Optional<Stock> findById(int id) {
        return jpa.findById(id);
    }

    @Override
    public Stock save(Stock stock) {
        return jpa.save(stock);
    }

    @Override
    public boolean deleteById(int id) {
        if (!jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }

    @Override
    public boolean existsByTicker(String ticker, Integer excludingId) {
        return excludingId == null
                ? jpa.existsByTickerIgnoreCase(ticker)
                : jpa.existsByTickerIgnoreCaseAndIdNot(ticker, excludingId);
    }
}
