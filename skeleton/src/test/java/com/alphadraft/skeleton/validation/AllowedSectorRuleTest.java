package com.alphadraft.skeleton.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.StockRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AllowedSectorRuleTest {

    private final AllowedSectorRule rule = new AllowedSectorRule();

    private Map<String, String> validateSector(String sector) {
        return rule.validate(new StockRequest("AAPL", "Apple Inc.", sector), null);
    }

    @Test
    void passesForEveryAllowedSector() {
        AllowedSectorRule.SECTORS.forEach(sector -> assertTrue(validateSector(sector).isEmpty(), sector));
    }

    @Test
    void hasTheElevenGicsSectors() {
        assertEquals(11, AllowedSectorRule.SECTORS.size());
    }

    @Test
    void rejectsUnknownSector() {
        assertEquals(Map.of("sector", "Choose a sector from the list."), validateSector("Crypto"));
    }

    @Test
    void sectorMustMatchExactly() {
        assertEquals(Map.of("sector", "Choose a sector from the list."), validateSector("technology"));
    }

    @Test
    void leavesMissingSectorToRequiredFieldsRule() {
        assertTrue(validateSector(null).isEmpty());
        assertTrue(validateSector("").isEmpty());
    }
}
