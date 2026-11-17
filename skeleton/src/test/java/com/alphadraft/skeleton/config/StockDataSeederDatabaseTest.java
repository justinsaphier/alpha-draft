package com.alphadraft.skeleton.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.JpaStockRepository;
import com.alphadraft.skeleton.repository.StockJpaRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

// Runs the seeder against a real (in-memory H2) database through the same JpaStockRepository
// the app uses, to show that restarting the app never duplicates the starter stocks.
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:seeder-db-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class StockDataSeederDatabaseTest {

    private static final List<String> STARTER_TICKERS = List.of("AAPL", "JPM", "JNJ", "XOM", "CAT", "NEE");

    @Autowired
    private StockJpaRepository stockJpaRepository;

    private JpaStockRepository repository;
    private StockDataSeeder seeder;

    @BeforeEach
    void setUp() {
        repository = new JpaStockRepository(stockJpaRepository);
        seeder = new StockDataSeeder(repository);
    }

    private List<String> tickers() {
        return repository.findAll().stream().map(Stock::getTicker).toList();
    }

    @Test
    void firstRunLoadsTheSixStarterStocksInOrder() {
        seeder.run();

        assertEquals(STARTER_TICKERS, tickers());
    }

    @Test
    void simulatedRestartsLeaveExactlySixStocks() {
        seeder.run();
        seeder.run();
        seeder.run();

        assertEquals(6, stockJpaRepository.count());
        assertEquals(STARTER_TICKERS, tickers());
    }

    @Test
    void seededIdsComeFromOneIncreasingSequence() {
        seeder.run();

        List<Integer> ids = repository.findAll().stream().map(Stock::getId).toList();
        for (int i = 1; i < ids.size(); i++) {
            assertEquals(ids.get(i - 1) + 1, ids.get(i));
        }
        assertTrue(ids.get(0) > 0);
    }

    @Test
    void stocksAddedAfterSeedingSurviveARestart() {
        seeder.run();
        Stock msft = repository.save(new Stock(0, "MSFT", "Microsoft Corporation", "Technology"));

        seeder.run();

        assertEquals(7, stockJpaRepository.count());
        assertTrue(msft.getId() > repository.findAll().get(5).getId());
    }

    @Test
    void seededTickersCountAsDuplicatesInAnyCase() {
        seeder.run();

        assertTrue(repository.existsByTicker("aapl", null));
        assertTrue(repository.existsByTicker("Nee", null));
        assertFalse(repository.existsByTicker("MSFT", null));
    }

    @Test
    void updatingASeededStockKeepsItsTickerFreeForItself() {
        seeder.run();
        int aaplId = repository.findAll().get(0).getId();

        assertFalse(repository.existsByTicker("AAPL", aaplId));
        assertTrue(repository.existsByTicker("JPM", aaplId));
    }
}