package com.alphadraft.skeleton.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.Stock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryStockRepositoryTest {

    private InMemoryStockRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryStockRepository();
    }

    @Test
    void findAllReturnsSeededStocksInIdOrder() {
        List<Integer> ids = repository.findAll().stream().map(Stock::getId).toList();

        assertEquals(List.of(1, 2, 3, 4, 5, 6), ids);
    }

    @Test
    void findByIdReturnsMatchingStock() {
        Stock stock = repository.findById(1).orElseThrow();

        assertEquals("AAPL", stock.getTicker());
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertTrue(repository.findById(99).isEmpty());
    }

    @Test
    void saveAddsNewStock() {
        Stock msft = new Stock(7, "MSFT", "Microsoft Corporation", "Technology");

        assertSame(msft, repository.save(msft));
        assertSame(msft, repository.findById(7).orElseThrow());
        assertEquals(7, repository.findAll().size());
    }

    @Test
    void saveReplacesStockWithSameId() {
        Stock renamed = new Stock(1, "AAPL", "Apple", "Technology");

        repository.save(renamed);

        assertEquals("Apple", repository.findById(1).orElseThrow().getName());
        assertEquals(6, repository.findAll().size());
    }

    @Test
    void findAllReturnsCopyThatDoesNotChangeStorage() {
        repository.findAll().clear();

        assertEquals(6, repository.findAll().size());
    }
}
