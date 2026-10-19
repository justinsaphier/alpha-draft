package com.alphadraft.skeleton.service;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.StockRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Hand-written test double: lets StockService be tested without the real storage.
class FakeStockRepository implements StockRepository {

    private final List<Stock> stocks = new ArrayList<>();

    FakeStockRepository(Stock... stocks) {
        this.stocks.addAll(List.of(stocks));
    }

    @Override
    public List<Stock> findAll() {
        return new ArrayList<>(stocks);
    }

    @Override
    public Optional<Stock> findById(int id) {
        return stocks.stream().filter(stock -> stock.getId() == id).findFirst();
    }

    @Override
    public Stock save(Stock stock) {
        stocks.removeIf(existing -> existing.getId() == stock.getId());
        stocks.add(stock);
        return stock;
    }
}
