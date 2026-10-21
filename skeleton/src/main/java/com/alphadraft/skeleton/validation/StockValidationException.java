package com.alphadraft.skeleton.validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Thrown when one or more StockValidationRules reject a stock.
public class StockValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public StockValidationException(Map<String, String> errors) {
        super("Stock failed validation: " + errors);
        // Keeps the order the rules reported errors in, so the response lists fields predictably.
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
