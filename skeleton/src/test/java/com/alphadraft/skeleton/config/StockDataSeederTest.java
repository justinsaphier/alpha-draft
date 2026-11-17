package com.alphadraft.skeleton.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.testsupport.FakeStockRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockDataSeederTest {

    // Records every save so the test can count calls, not just the final contents.
    private static class RecordingStockRepository extends FakeStockRepository {

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

    @Test
    void runningTwiceStillLeavesExactlySixStocks() {
        RecordingStockRepository repository = new RecordingStockRepository();
        StockDataSeeder seeder = new StockDataSeeder(repository);

        seeder.run();
        seeder.run();

        assertEquals(6, repository.saved.size());
        assertEquals(6, repository.findAll().size());
    }

    @Test
    void runDoesNothingWhenTheTableAlreadyHasStocks() {
        RecordingStockRepository repository = new RecordingStockRepository();
        repository.save(new Stock(0, "MSFT", "Microsoft Corporation", "Technology"));
        repository.saved.clear();

        new StockDataSeeder(repository).run();

        assertTrue(repository.saved.isEmpty());
        assertEquals(List.of("MSFT"), repository.findAll().stream().map(Stock::getTicker).toList());
    }

    @Test
    void runDoesNotRefillAfterAStarterStockIsDeleted() {
        RecordingStockRepository repository = new RecordingStockRepository();
        StockDataSeeder seeder = new StockDataSeeder(repository);
        seeder.run();
        repository.deleteById(repository.findAll().get(0).getId());

        seeder.run();

        assertEquals(5, repository.findAll().size());
    }

    @Test
    void runSavesCopiesSoTheStarterConstantsKeepIdZero() {
        RecordingStockRepository repository = new RecordingStockRepository();

        new StockDataSeeder(repository).run();

        assertTrue(StockDataSeeder.STARTER_STOCKS.stream().allMatch(stock -> stock.getId() == 0));
        assertTrue(repository.saved.stream().noneMatch(StockDataSeeder.STARTER_STOCKS::contains));
    }
}