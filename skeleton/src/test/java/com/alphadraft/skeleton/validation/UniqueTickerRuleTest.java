package com.alphadraft.skeleton.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.model.StockRequest;
import com.alphadraft.skeleton.repository.InMemoryStockRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UniqueTickerRuleTest {

    private UniqueTickerRule rule;

    @BeforeEach
    void setUp() {
        InMemoryStockRepository repository = new InMemoryStockRepository();
        repository.save(new Stock(1, "AAPL", "Apple Inc.", "Technology"));
        repository.save(new Stock(2, "JPM", "JPMorgan Chase & Co.", "Financials"));
        rule = new UniqueTickerRule(repository);
    }

    private static StockRequest withTicker(String ticker) {
        return new StockRequest(ticker, "Some Company", "Technology");
    }

    @Test
    void passesForNewTicker() {
        assertTrue(rule.validate(withTicker("MSFT"), null).isEmpty());
    }

    @Test
    void rejectsTickerAlreadyInUseWhenCreating() {
        assertEquals(Map.of("ticker", "Ticker already exists."), rule.validate(withTicker("AAPL"), null));
    }

    @Test
    void comparesTickersIgnoringCase() {
        assertEquals(Map.of("ticker", "Ticker already exists."), rule.validate(withTicker("aapl"), null));
    }

    @Test
    void letsAStockKeepItsOwnTickerWhenUpdating() {
        assertTrue(rule.validate(withTicker("AAPL"), 1).isEmpty());
    }

    @Test
    void rejectsTakingAnotherStocksTickerWhenUpdating() {
        assertEquals(Map.of("ticker", "Ticker already exists."), rule.validate(withTicker("JPM"), 1));
    }

    @Test
    void leavesMissingTickerToRequiredFieldsRule() {
        assertTrue(rule.validate(withTicker(null), null).isEmpty());
        assertTrue(rule.validate(withTicker(" "), null).isEmpty());
    }
}
