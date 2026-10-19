package com.alphadraft.skeleton.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.model.StockRequest;
import com.alphadraft.skeleton.validation.StockValidationException;
import com.alphadraft.skeleton.validation.StockValidationRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StockServiceTest {

    private final Stock msft = new Stock(10, "MSFT", "Microsoft Corporation", "Technology");
    private final Stock ko = new Stock(11, "KO", "The Coca-Cola Company", "Consumer Staples");
    private final StockRequest nvda = new StockRequest("NVDA", "NVIDIA Corporation", "Technology");

    // Stand-in rule: records what it was asked to check and returns fixed errors.
    private static class RecordingRule implements StockValidationRule {

        private final Map<String, String> errors;
        private final List<StockRequest> checked = new ArrayList<>();
        private final List<Integer> ids = new ArrayList<>();

        RecordingRule(Map<String, String> errors) {
            this.errors = errors;
        }

        @Override
        public Map<String, String> validate(StockRequest stock, Integer idBeingUpdated) {
            checked.add(stock);
            ids.add(idBeingUpdated);
            return errors;
        }
    }

    private static StockService service(FakeStockRepository repository, StockValidationRule... rules) {
        return new StockService(repository, List.of(rules));
    }

    @Test
    void getAllStocksReturnsWhatTheRepositoryHolds() {
        StockService service = service(new FakeStockRepository(msft, ko));

        assertEquals(List.of(msft, ko), service.getAllStocks());
    }

    @Test
    void getAllStocksReturnsEmptyListWhenRepositoryIsEmpty() {
        StockService service = service(new FakeStockRepository());

        assertTrue(service.getAllStocks().isEmpty());
    }

    @Test
    void getStockByIdReturnsStockFromRepository() {
        StockService service = service(new FakeStockRepository(msft, ko));

        assertSame(ko, service.getStockById(11).orElseThrow());
    }

    @Test
    void getStockByIdReturnsEmptyForUnknownId() {
        StockService service = service(new FakeStockRepository(msft));

        assertTrue(service.getStockById(99).isEmpty());
    }

    @Test
    void createStockRunsEveryRuleBeforeSaving() {
        RecordingRule first = new RecordingRule(Map.of());
        RecordingRule second = new RecordingRule(Map.of());
        StockService service = service(new FakeStockRepository(msft), first, second);

        service.createStock(nvda);

        assertEquals(List.of(nvda), first.checked);
        assertEquals(List.of(nvda), second.checked);
        assertEquals(1, first.ids.size());
        assertEquals(null, first.ids.get(0));
    }

    @Test
    void createStockSavesWithNextIdAndCleanedValues() {
        FakeStockRepository repository = new FakeStockRepository(msft, ko);
        StockService service = service(repository);

        Stock created = service.createStock(new StockRequest("  nvda ", " NVIDIA Corporation ", "Technology"));

        assertEquals(12, created.getId());
        assertEquals("NVDA", created.getTicker());
        assertEquals("NVIDIA Corporation", created.getName());
        assertSame(created, repository.findById(12).orElseThrow());
    }

    @Test
    void createStockStartsIdsAtOneWhenEmpty() {
        StockService service = service(new FakeStockRepository());

        assertEquals(1, service.createStock(nvda).getId());
    }

    @Test
    void createStockThrowsAndSavesNothingWhenARuleFails() {
        FakeStockRepository repository = new FakeStockRepository(msft);
        StockService service = service(repository, new RecordingRule(Map.of("ticker", "Ticker already exists.")));

        StockValidationException e = assertThrows(StockValidationException.class, () -> service.createStock(nvda));

        assertEquals(Map.of("ticker", "Ticker already exists."), e.getErrors());
        assertEquals(List.of(msft), repository.findAll());
    }

    @Test
    void errorsFromAllRulesAreCombinedKeepingTheFirstPerField() {
        StockService service = service(new FakeStockRepository(),
                new RecordingRule(Map.of("ticker", "Ticker is required.")),
                new RecordingRule(Map.of("ticker", "Ticker already exists.", "sector", "Choose a sector from the list.")));

        StockValidationException e = assertThrows(StockValidationException.class, () -> service.createStock(nvda));

        assertEquals(Map.of("ticker", "Ticker is required.", "sector", "Choose a sector from the list."), e.getErrors());
    }

    @Test
    void updateStockRunsEveryRuleWithTheIdBeingUpdated() {
        RecordingRule first = new RecordingRule(Map.of());
        RecordingRule second = new RecordingRule(Map.of());
        StockService service = service(new FakeStockRepository(msft), first, second);

        service.updateStock(10, nvda);

        assertEquals(List.of(10), first.ids);
        assertEquals(List.of(10), second.ids);
    }

    @Test
    void updateStockSavesChangesUnderTheSameId() {
        FakeStockRepository repository = new FakeStockRepository(msft);
        StockService service = service(repository);

        Stock updated = service.updateStock(10, nvda).orElseThrow();

        assertEquals(10, updated.getId());
        assertEquals("NVDA", repository.findById(10).orElseThrow().getTicker());
    }

    @Test
    void updateStockReturnsEmptyForUnknownIdWithoutRunningRules() {
        RecordingRule rule = new RecordingRule(Map.of());
        StockService service = service(new FakeStockRepository(msft), rule);

        assertTrue(service.updateStock(99, nvda).isEmpty());
        assertTrue(rule.checked.isEmpty());
    }

    @Test
    void deleteStockRemovesStockAndReturnsTrue() {
        FakeStockRepository repository = new FakeStockRepository(msft, ko);
        StockService service = service(repository);

        assertTrue(service.deleteStock(10));
        assertEquals(List.of(ko), repository.findAll());
    }

    @Test
    void deleteStockReturnsFalseForUnknownId() {
        StockService service = service(new FakeStockRepository(msft));

        assertFalse(service.deleteStock(99));
    }

    @Test
    void createStockDoesNotReuseTheIdOfADeletedStock() {
        StockService service = service(new FakeStockRepository(msft));
        Stock first = service.createStock(nvda);

        service.deleteStock(first.getId());
        Stock second = service.createStock(new StockRequest("KO", "The Coca-Cola Company", "Consumer Staples"));

        assertEquals(11, first.getId());
        assertEquals(12, second.getId());
    }

    @Test
    void updateStockThrowsAndKeepsTheOldStockWhenARuleFails() {
        FakeStockRepository repository = new FakeStockRepository(msft);
        StockService service = service(repository, new RecordingRule(Map.of("name", "Company name is required.")));

        assertThrows(StockValidationException.class, () -> service.updateStock(10, nvda));

        assertSame(msft, repository.findById(10).orElseThrow());
    }
}
