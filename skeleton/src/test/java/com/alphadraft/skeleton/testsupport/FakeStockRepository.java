package com.alphadraft.skeleton.testsupport;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.StockRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Hand-written test double: an in-memory StockRepository for unit tests that don't need a
// database. Like the real database, a stock saved with id 0 gets the next id from a
// sequence that never reuses ids, even after deletes.
public class FakeStockRepository implements StockRepository {

    private final List<Stock> stocks = new ArrayList<>();
    private int lastId;

    public FakeStockRepository(Stock... stocks) {
        for (Stock stock : stocks) {
            this.stocks.add(stock);
            lastId = Math.max(lastId, stock.getId());
        }
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
        Stock toStore = stock;
        if (stock.getId() == 0) {
            lastId++;
            toStore = new Stock(lastId, stock.getTicker(), stock.getName(), stock.getSector());
        } else {
            lastId = Math.max(lastId, stock.getId());
        }
        int id = toStore.getId();
        stocks.removeIf(existing -> existing.getId() == id);
        stocks.add(toStore);
        return toStore;
    }

    @Override
    public boolean deleteById(int id) {
        return stocks.removeIf(stock -> stock.getId() == id);
    }

    @Override
    public boolean existsByTicker(String ticker, Integer excludingId) {
        return stocks.stream()
                .filter(stock -> excludingId == null || stock.getId() != excludingId)
                .anyMatch(stock -> stock.getTicker().equalsIgnoreCase(ticker));
    }
}
