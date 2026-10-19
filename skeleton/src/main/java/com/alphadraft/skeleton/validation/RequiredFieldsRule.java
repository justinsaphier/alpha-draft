package com.alphadraft.skeleton.validation;

import com.alphadraft.skeleton.model.StockRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Runs first so missing fields are reported before any other rule looks at them.
@Component
@Order(1)
public class RequiredFieldsRule implements StockValidationRule {

    @Override
    public Map<String, String> validate(StockRequest stock, Integer idBeingUpdated) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(stock.ticker())) {
            errors.put("ticker", "Ticker is required.");
        }
        if (isBlank(stock.name())) {
            errors.put("name", "Company name is required.");
        }
        if (isBlank(stock.sector())) {
            errors.put("sector", "Sector is required.");
        }
        return errors;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
