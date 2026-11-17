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
## SCRUM-32: Extract shared blank-field check from validation rules

**Smell:** Duplicated Code. The "null or blank" check was written in `RequiredFieldsRule` (as a private
helper), `AllowedSectorRule` and `UniqueTickerRule` (inline). `StockRequest.normalized()` repeated the
same null handling (`x == null ? null : x.trim()`) for each field.

**Refactoring:** Extract Method. Added `FieldChecks.isBlank(String)` in the validation package. All three
rules call it, and `StockRequest.normalized()` now uses a `trimmed(String)` helper.

**Before** (`AllowedSectorRule.validate`):

```java
if (stock.sector() == null || stock.sector().isBlank() || SECTORS.contains(stock.sector())) {
    return Map.of();
}
```

**Before** (`StockRequest.normalized`):

```java
return new StockRequest(
        ticker == null ? null : ticker.trim().toUpperCase(),
        name == null ? null : name.trim(),
        sector == null ? null : sector.trim());
```

**After** (`AllowedSectorRule.validate`):

```java
if (FieldChecks.isBlank(stock.sector()) || SECTORS.contains(stock.sector())) {
    return Map.of();
}
```

**After** (`FieldChecks`):

```java
public static boolean isBlank(String value) {
    return value == null || value.isBlank();
}
```

**After** (`StockRequest.normalized`):

```java
String cleanTicker = trimmed(ticker);
return new StockRequest(
        cleanTicker == null ? null : cleanTicker.toUpperCase(),
        trimmed(name),
        trimmed(sector));
```

**Tests:** `FieldChecksTest` covers null, empty, whitespace and text. `StockRequestTest` covers trimming,
uppercasing the ticker, and keeping missing fields null. The existing rule tests are unchanged and still
pass. The full backend suite is green (63 tests).

**Commit:** [964e4d9](https://github.com/justinsaphier/alpha-draft/commit/964e4d9) — SCRUM-32: Extract shared blank-field check from validation rules
## SCRUM-33: Move 400 error handling into a @RestControllerAdvice

**Smell:** Divergent Change. `StockController` changed for two unrelated reasons: when the endpoints
changed, and when the way validation errors are reported changed. Its two `@ExceptionHandler` methods
and the error-body helper sat beside the endpoints.

**Refactoring:** Extract Class. Moved the handlers into `StockErrorHandler`, a `@RestControllerAdvice`
scoped with `assignableTypes = StockController.class`. `StockController` now holds only its endpoints,
under one class-level `@RequestMapping("/stocks")`.

**Before** (`StockController`, excerpt):

```java
@RestController
public class StockController {

    @GetMapping("/stocks")
    public List<Stock> getStocks() { ... }

    @PostMapping("/stocks")
    public ResponseEntity<Stock> createStock(...) { ... }

    @ExceptionHandler(StockValidationException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleValidation(StockValidationException e) {
        return errors(e.getErrors());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleUnreadableBody(HttpMessageNotReadableException e) {
        return errors(Map.of("body", "Request body must be a JSON stock with ticker, name and sector."));
    }
    ...
}
```

**After** (`StockController`, excerpt):

```java
@RestController
@RequestMapping("/stocks")
public class StockController {

    @GetMapping
    public List<Stock> getStocks() { ... }

    @PostMapping
    public ResponseEntity<Stock> createStock(...) { ... }
    ...
}
```

**After** (`StockErrorHandler`):

```java
@RestControllerAdvice(assignableTypes = StockController.class)
public class StockErrorHandler {

    @ExceptionHandler(StockValidationException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleValidation(StockValidationException e) {
        return errors(e.getErrors());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleUnreadableBody(HttpMessageNotReadableException e) {
        return errors(Map.of("body", "Request body must be a JSON stock with ticker, name and sector."));
    }
    ...
}
```

**Tests:** `StockControllerTest` is unchanged and passes. Its 400 cases (validation errors, malformed or
missing JSON body) run through the advice, because `@WebMvcTest` loads `@ControllerAdvice` beans. The full
backend suite is green (57 tests).

**Commit:** [3703aeb](https://github.com/justinsaphier/alpha-draft/commit/3703aeb) — SCRUM-33: Move 400 error handling into a @RestControllerAdvice
