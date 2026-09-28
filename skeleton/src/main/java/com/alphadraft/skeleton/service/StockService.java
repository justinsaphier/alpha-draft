package com.alphadraft.skeleton.service;

import com.alphadraft.skeleton.model.Stock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    // Keyed by id so stocks can be changed later; sorted map keeps GET /stocks in id order.
    private final Map<Integer, Stock> stocks = new ConcurrentSkipListMap<>();

    public StockService() {
        List.of(
                new Stock(1, "AAPL", "Apple Inc.", "Technology"),
                new Stock(2, "JPM", "JPMorgan Chase & Co.", "Financials"),
                new Stock(3, "JNJ", "Johnson & Johnson", "Healthcare"),
                new Stock(4, "XOM", "Exxon Mobil Corporation", "Energy"),
                new Stock(5, "CAT", "Caterpillar Inc.", "Industrials"),
                new Stock(6, "NEE", "NextEra Energy, Inc.", "Utilities"))
                .forEach(stock -> stocks.put(stock.getId(), stock));
    }

    public List<Stock> getAllStocks() {
        return new ArrayList<>(stocks.values());
    }

    public Optional<Stock> getStockById(int id) {
        return Optional.ofNullable(stocks.get(id));
    }
}
