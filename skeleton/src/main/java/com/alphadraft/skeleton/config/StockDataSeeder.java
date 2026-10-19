package com.alphadraft.skeleton.config;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.StockRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Loads the starter stocks when the app starts, so neither StockService nor the
// repository has to know what the starting data is.
@Component
public class StockDataSeeder implements CommandLineRunner {

    static final List<Stock> STARTER_STOCKS = List.of(
            new Stock(1, "AAPL", "Apple Inc.", "Technology"),
            new Stock(2, "JPM", "JPMorgan Chase & Co.", "Financials"),
            new Stock(3, "JNJ", "Johnson & Johnson", "Healthcare"),
            new Stock(4, "XOM", "Exxon Mobil Corporation", "Energy"),
            new Stock(5, "CAT", "Caterpillar Inc.", "Industrials"),
            new Stock(6, "NEE", "NextEra Energy, Inc.", "Utilities"));

    private final StockRepository stockRepository;

    public StockDataSeeder(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public void run(String... args) {
        STARTER_STOCKS.forEach(stockRepository::save);
    }
}
