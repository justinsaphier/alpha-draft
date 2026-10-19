package com.alphadraft.skeleton.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.InMemoryStockRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockDataSeederTest {

    // Records every save so the test can count calls, not just the final contents.
    private static class RecordingStockRepository extends InMemoryStockRepository {

        private final List<Stock> saved = new ArrayList<>();

        @Override
        public Stock save(Stock stock) {
            saved.add(stock);
            return super.save(stock);
        }
    }

    @Test
    void runSavesExactlySixStocks() {
        RecordingStockRepository repository = new RecordingStockRepository();

        new StockDataSeeder(repository).run();

        assertEquals(6, repository.saved.size());
        assertEquals(6, repository.findAll().size());
    }

    @Test
    void runSavesTheStarterStocks() {
        RecordingStockRepository repository = new RecordingStockRepository();

        new StockDataSeeder(repository).run();

        List<String> tickers = repository.findAll().stream().map(Stock::getTicker).toList();
        assertEquals(List.of("AAPL", "JPM", "JNJ", "XOM", "CAT", "NEE"), tickers);
    }
}
