package com.alphadraft.skeleton.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.Stock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

// Runs against an in-memory H2 database (DataJpaTest replaces the datasource), so no
// Supabase credentials are needed. Each test runs in a transaction that is rolled back.
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:jpa-stock-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class JpaStockRepositoryTest {

    @Autowired
    private StockJpaRepository stockJpaRepository;

    private JpaStockRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaStockRepository(stockJpaRepository);
    }

    @Test
    void saveLetsTheDatabaseGenerateTheId() {
        Stock saved = repository.save(new Stock(0, "AAPL", "Apple Inc.", "Technology"));

        assertTrue(saved.getId() > 0);
        assertEquals("AAPL", repository.findById(saved.getId()).orElseThrow().getTicker());
    }

    @Test
    void generatedIdsIncreaseWithEachSave() {
        int first = repository.save(new Stock(0, "AAPL", "Apple Inc.", "Technology")).getId();
        int second = repository.save(new Stock(0, "JPM", "JPMorgan Chase & Co.", "Financials")).getId();

        assertTrue(second > first);
    }

    @Test
    void idsAreNotReusedAfterDelete() {
        int deleted = repository.save(new Stock(0, "AAPL", "Apple Inc.", "Technology")).getId();
        repository.deleteById(deleted);

        int next = repository.save(new Stock(0, "JPM", "JPMorgan Chase & Co.", "Financials")).getId();

        assertTrue(next > deleted);
    }

    @Test
    void findAllReturnsStocksInIdOrder() {
        Stock aapl = repository.save(new Stock(0, "AAPL", "Apple Inc.", "Technology"));
        Stock jpm = repository.save(new Stock(0, "JPM", "JPMorgan Chase & Co.", "Financials"));

        assertEquals(List.of(aapl.getId(), jpm.getId()),
                repository.findAll().stream().map(Stock::getId).toList());
    }

    @Test
    void saveOfExistingIdUpdatesTheRow() {
        Stock saved = repository.save(new Stock(0, "AAPL", "Apple Inc.", "Technology"));

        repository.save(new Stock(saved.getId(), "AAPL", "Apple", "Technology"));

        assertEquals("Apple", repository.findById(saved.getId()).orElseThrow().getName());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void deleteByIdRemovesTheStockAndReturnsFalseWhenMissing() {
        Stock saved = repository.save(new Stock(0, "AAPL", "Apple Inc.", "Technology"));

        assertTrue(repository.deleteById(saved.getId()));
        assertTrue(repository.findById(saved.getId()).isEmpty());
        assertFalse(repository.deleteById(saved.getId()));
    }

    @Test
    void existsByTickerIgnoresCase() {
        repository.save(new Stock(0, "AAPL", "Apple Inc.", "Technology"));

        assertTrue(repository.existsByTicker("aapl", null));
        assertTrue(repository.existsByTicker("AaPl", null));
        assertFalse(repository.existsByTicker("MSFT", null));
    }

    @Test
    void existsByTickerCanIgnoreTheStockBeingUpdated() {
        Stock saved = repository.save(new Stock(0, "AAPL", "Apple Inc.", "Technology"));

        assertFalse(repository.existsByTicker("AAPL", saved.getId()));
        assertFalse(repository.existsByTicker("aapl", saved.getId()));
    }
}
