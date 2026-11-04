# Refactoring Log

Each entry records one refactoring: the code smell, the code before, the code after, and the commit that made the change.

## SCRUM-31: Move stock id generation into StockRepository

**Smell:** Feature Envy. `StockService` scanned the whole repository (`findAll()`) and kept its own
`lastIssuedId` to pick the next id. Id allocation is storage knowledge, so it belongs with the repository.

**Refactoring:** Move Method. Added `StockRepository.nextId()`. `InMemoryStockRepository` keeps a
high-water mark: `save()` raises it and `nextId()` increments it. Deleting a stock never lowers it, so
an id is never reused.

**Before** (`StockService`):

```java
private int lastIssuedId;

private int nextId() {
    int highestStored = stockRepository.findAll().stream().mapToInt(Stock::getId).max().orElse(0);
    lastIssuedId = Math.max(lastIssuedId, highestStored) + 1;
    return lastIssuedId;
}

// in createStock
return stockRepository.save(new Stock(nextId(), stock.ticker(), stock.name(), stock.sector()));
```

**After** (`StockService`):

```java
// in createStock
return stockRepository.save(new Stock(stockRepository.nextId(), stock.ticker(), stock.name(), stock.sector()));
```

**After** (`InMemoryStockRepository`):

```java
private final AtomicInteger highestId = new AtomicInteger();

@Override
public Stock save(Stock stock) {
    stocks.put(stock.getId(), stock);
    highestId.accumulateAndGet(stock.getId(), Math::max);
    return stock;
}

@Override
public int nextId() {
    return highestId.incrementAndGet();
}
```

**Tests:** `InMemoryStockRepositoryTest` covers the first id, ids after the highest saved id, and ids
that are not reused after a delete. `StockServiceTest.createStockDoesNotReuseTheIdOfADeletedStock` still
passes against the fake. The full backend suite is green (60 tests).

**Commit:** [7336381](https://github.com/justinsaphier/alpha-draft/commit/7336381) — SCRUM-31: Move stock id generation into StockRepository
