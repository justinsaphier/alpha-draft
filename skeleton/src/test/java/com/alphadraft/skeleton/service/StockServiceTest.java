package com.alphadraft.skeleton.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.Stock;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockServiceTest {

    private final Stock msft = new Stock(10, "MSFT", "Microsoft Corporation", "Technology");
    private final Stock ko = new Stock(11, "KO", "The Coca-Cola Company", "Consumer Staples");

    @Test
    void getAllStocksReturnsWhatTheRepositoryHolds() {
        StockService service = new StockService(new FakeStockRepository(msft, ko));

        assertEquals(List.of(msft, ko), service.getAllStocks());
    }

    @Test
    void getAllStocksReturnsEmptyListWhenRepositoryIsEmpty() {
        StockService service = new StockService(new FakeStockRepository());

        assertTrue(service.getAllStocks().isEmpty());
    }

    @Test
    void getStockByIdReturnsStockFromRepository() {
        StockService service = new StockService(new FakeStockRepository(msft, ko));

        assertSame(ko, service.getStockById(11).orElseThrow());
    }

    @Test
    void getStockByIdReturnsEmptyForUnknownId() {
        StockService service = new StockService(new FakeStockRepository(msft));

        assertTrue(service.getStockById(99).isEmpty());
    }
}
