# Refactoring Log

Each entry records one refactoring: the code smell, the code before, the code after, and the commit that made the change.

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
