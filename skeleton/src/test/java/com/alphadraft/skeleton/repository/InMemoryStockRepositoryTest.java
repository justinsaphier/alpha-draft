package com.alphadraft.skeleton.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.Stock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryStockRepositoryTest {

    private final Stock aapl = new Stock(1, "AAPL", "Apple Inc.", "Technology");
    private final Stock jpm = new Stock(2, "JPM", "JPMorgan Chase & Co.", "Financials");
    private final Stock msft = new Stock(7, "MSFT", "Microsoft Corporation", "Technology");

    private InMemoryStockRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryStockRepository();
    }

    @Test
    void startsEmpty() {
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void findAllReturnsStocksInIdOrder() {
        repository.save(msft);
        repository.save(jpm);
        repository.save(aapl);

        List<Integer> ids = repository.findAll().stream().map(Stock::getId).toList();

        assertEquals(List.of(1, 2, 7), ids);
    }

    @Test
    void findByIdReturnsMatchingStock() {
        repository.save(aapl);

        assertSame(aapl, repository.findById(1).orElseThrow());
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        repository.save(aapl);

        assertTrue(repository.findById(99).isEmpty());
    }

    @Test
    void saveReturnsTheSavedStock() {
        assertSame(msft, repository.save(msft));
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void saveReplacesStockWithSameId() {
        repository.save(aapl);
        repository.save(new Stock(1, "AAPL", "Apple", "Technology"));

        assertEquals("Apple", repository.findById(1).orElseThrow().getName());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void deleteByIdRemovesStockAndReturnsTrue() {
        repository.save(aapl);
        repository.save(jpm);

        assertTrue(repository.deleteById(1));
        assertEquals(List.of(jpm), repository.findAll());
    }

    @Test
    void deleteByIdReturnsFalseForUnknownId() {
        repository.save(aapl);

        assertFalse(repository.deleteById(99));
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void findAllReturnsCopyThatDoesNotChangeStorage() {
        repository.save(aapl);

        repository.findAll().clear();

        assertEquals(1, repository.findAll().size());
    }
}
