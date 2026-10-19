package com.alphadraft.skeleton.validation;

import com.alphadraft.skeleton.model.StockRequest;
import java.util.Map;

// Strategy pattern: each rule is one interchangeable check with the same shape.
// StockService runs every rule without knowing what any of them check, so a new
// rule is a new class and StockService does not change.
public interface StockValidationRule {

    /**
     * Checks a stock that is about to be created or updated.
     *
     * @param stock the normalized stock values
     * @param idBeingUpdated the id of the stock being updated, or null when creating
     * @return a { field: message } map for each problem; empty when the stock passes
     */
    Map<String, String> validate(StockRequest stock, Integer idBeingUpdated);
}
