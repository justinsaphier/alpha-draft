package com.alphadraft.skeleton.validation;

import com.alphadraft.skeleton.model.StockRequest;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class AllowedSectorRule implements StockValidationRule {

    // The 11 GICS sectors; matches frontend/src/constants/sectors.js.
    public static final List<String> SECTORS = List.of(
            "Technology",
            "Financials",
            "Real Estate",
            "Healthcare",
            "Consumer Staples",
            "Energy",
            "Materials",
            "Industrials",
            "Consumer Discretionary",
            "Utilities",
            "Communication Services");

    @Override
    public Map<String, String> validate(StockRequest stock, Integer idBeingUpdated) {
        // A missing sector is reported by RequiredFieldsRule, not here.
        if (stock.sector() == null || stock.sector().isBlank() || SECTORS.contains(stock.sector())) {
            return Map.of();
        }
        return Map.of("sector", "Choose a sector from the list.");
    }
}
