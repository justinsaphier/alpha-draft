package com.alphadraft.skeleton.repository;

import com.alphadraft.skeleton.model.Stock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryStockRepository implements StockRepository {

    // Keyed by id so stocks can be changed later; sorted map keeps findAll in id order.
    private final Map<Integer, Stock> stocks = new ConcurrentSkipListMap<>();

    public InMemoryStockRepository() {
        List.of(
                new Stock(1, "AAPL", "Apple Inc.", "Technology"),
                new Stock(2, "JPM", "JPMorgan Chase & Co.", "Financials"),
                new Stock(3, "JNJ", "Johnson & Johnson", "Healthcare"),
                new Stock(4, "XOM", "Exxon Mobil Corporation", "Energy"),
                new Stock(5, "CAT", "Caterpillar Inc.", "Industrials"),
                new Stock(6, "NEE", "NextEra Energy, Inc.", "Utilities"))
                .forEach(this::save);
    }

    @Override
    public List<Stock> findAll() {
        return new ArrayList<>(stocks.values());
    }

    @Override
    public Optional<Stock> findById(int id) {
        return Optional.ofNullable(stocks.get(id));
    }

    @Override
    public Stock save(Stock stock) {
        stocks.put(stock.getId(), stock);
        return stock;
    }
}
