package com.alphadraft.skeleton.validation;

import java.util.Map;

// Thrown when one or more StockValidationRules reject a stock.
public class StockValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public StockValidationException(Map<String, String> errors) {
        super("Stock failed validation: " + errors);
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
