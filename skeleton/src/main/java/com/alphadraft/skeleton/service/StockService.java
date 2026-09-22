package com.alphadraft.skeleton.service;

import com.alphadraft.skeleton.model.Stock;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    private final List<Stock> stocks = List.of(
            new Stock(1, "AAPL", "Apple Inc.", "Technology"),
            new Stock(2, "JPM", "JPMorgan Chase & Co.", "Financials"),
            new Stock(3, "JNJ", "Johnson & Johnson", "Healthcare"),
            new Stock(4, "XOM", "Exxon Mobil Corporation", "Energy"),
            new Stock(5, "CAT", "Caterpillar Inc.", "Industrials"),
            new Stock(6, "NEE", "NextEra Energy, Inc.", "Utilities"));

    public List<Stock> getAllStocks() {
        return stocks;
    }
}
