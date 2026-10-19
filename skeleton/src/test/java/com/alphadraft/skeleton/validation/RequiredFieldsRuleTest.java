package com.alphadraft.skeleton.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.StockRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequiredFieldsRuleTest {

    private final RequiredFieldsRule rule = new RequiredFieldsRule();

    @Test
    void passesWhenEveryFieldIsFilledIn() {
        assertTrue(rule.validate(new StockRequest("AAPL", "Apple Inc.", "Technology"), null).isEmpty());
    }

    @Test
    void reportsEachMissingField() {
        Map<String, String> errors = rule.validate(new StockRequest(null, null, null), null);

        assertEquals(Map.of(
                "ticker", "Ticker is required.",
                "name", "Company name is required.",
                "sector", "Sector is required."), errors);
    }

    @Test
    void treatsBlankTextAsMissing() {
        Map<String, String> errors = rule.validate(new StockRequest("AAPL", "   ", ""), null);

        assertEquals(Map.of(
                "name", "Company name is required.",
                "sector", "Sector is required."), errors);
    }
}
