# Refactoring Log

Each entry records one refactoring: the code smell, the code before, the code after, and the commit that made the change.

## SCRUM-30: Move duplicate-ticker lookup into StockRepository

**Smell:** Feature Envy. `UniqueTickerRule` reached into the stock list itself (`findAll()`) and
compared tickers, which is data-access work that belongs to the repository.

**Refactoring:** Move Method. Added `StockRepository.existsByTicker(ticker, excludingId)`, implemented
by `InMemoryStockRepository` and the test double `FakeStockRepository`. `UniqueTickerRule` now asks
the repository and no longer calls `findAll()`.

**Before** (`UniqueTickerRule.validate`):

```java
boolean taken = stockRepository.findAll().stream()
        .filter(existing -> idBeingUpdated == null || existing.getId() != idBeingUpdated)
        .anyMatch(existing -> existing.getTicker().equalsIgnoreCase(stock.ticker()));
```

**After** (`UniqueTickerRule.validate`):

```java
boolean taken = stockRepository.existsByTicker(stock.ticker(), idBeingUpdated);
```

**After** (`InMemoryStockRepository`):

```java
@Override
public boolean existsByTicker(String ticker, Integer excludingId) {
    return stocks.values().stream()
            .filter(stock -> excludingId == null || stock.getId() != excludingId)
            .anyMatch(stock -> stock.getTicker().equalsIgnoreCase(ticker));
}
```

**Tests:** `InMemoryStockRepositoryTest` covers case-insensitive matches, free tickers, and the excluded
stock. `UniqueTickerRuleTest` is unchanged and still passes. The full backend suite is green (60 tests).

**Commit:** [5fde43c](https://github.com/justinsaphier/alpha-draft/commit/5fde43c) — SCRUM-30: Move duplicate-ticker lookup into StockRepository
