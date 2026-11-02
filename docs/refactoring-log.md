# Refactoring Log

Each entry records one refactoring: the code smell, the code before, the code after, and the commit that made the change.

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
