package com.alphadraft.skeleton.config;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.StockRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Loads the starter stocks when the app starts, so neither StockService nor the
// repository has to know what the starting data is. The table now lives in Supabase and
// survives restarts, so the seeder only fills an empty table: restarting never adds
// duplicates, and stocks an admin added or deleted are left alone.
@Component
public class StockDataSeeder implements CommandLineRunner {

    // Id 0 lets the database assign ids in insert order (1..6 on an empty table).
    static final List<Stock> STARTER_STOCKS = List.of(
            new Stock(0, "AAPL", "Apple Inc.", "Technology"),
            new Stock(0, "JPM", "JPMorgan Chase & Co.", "Financials"),
            new Stock(0, "JNJ", "Johnson & Johnson", "Healthcare"),
            new Stock(0, "XOM", "Exxon Mobil Corporation", "Energy"),
            new Stock(0, "CAT", "Caterpillar Inc.", "Industrials"),
            new Stock(0, "NEE", "NextEra Energy, Inc.", "Utilities"));

    private final StockRepository stockRepository;

    public StockDataSeeder(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public void run(String... args) {
        if (!stockRepository.findAll().isEmpty()) {
            return;
        }
        // Save a fresh copy of each starter stock. JPA writes the generated id back into the
        // object it saves, so saving the shared constants would change them for later runs.
        STARTER_STOCKS.forEach(stock -> stockRepository.save(
                new Stock(0, stock.getTicker(), stock.getName(), stock.getSector())));
    }
}