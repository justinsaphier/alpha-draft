package com.alphadraft.skeleton.service;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.model.StockRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    // Keyed by id so stocks can be changed later; sorted map keeps GET /stocks in id order.
    private final Map<Integer, Stock> stocks = new ConcurrentSkipListMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public StockService() {
        List.of(
                new StockRequest("AAPL", "Apple Inc.", "Technology"),
                new StockRequest("JPM", "JPMorgan Chase & Co.", "Financials"),
                new StockRequest("JNJ", "Johnson & Johnson", "Healthcare"),
                new StockRequest("XOM", "Exxon Mobil Corporation", "Energy"),
                new StockRequest("CAT", "Caterpillar Inc.", "Industrials"),
                new StockRequest("NEE", "NextEra Energy, Inc.", "Utilities"))
                .forEach(this::createStock);
    }

    public List<Stock> getAllStocks() {
        return new ArrayList<>(stocks.values());
    }

    public Optional<Stock> getStockById(int id) {
        return Optional.ofNullable(stocks.get(id));
    }

    public Stock createStock(StockRequest request) {
        int id = nextId.getAndIncrement();
        Stock stock = new Stock(id, request.ticker(), request.name(), request.sector());
        stocks.put(id, stock);
        return stock;
    }

    public Optional<Stock> updateStock(int id, StockRequest request) {
        return Optional.ofNullable(stocks.computeIfPresent(id,
                (key, existing) -> new Stock(id, request.ticker(), request.name(), request.sector())));
    }

    public boolean deleteStock(int id) {
        return stocks.remove(id) != null;
    }
}
