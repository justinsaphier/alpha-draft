package com.alphadraft.skeleton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphadraft.skeleton.model.StockRequest;
import com.alphadraft.skeleton.repository.InMemoryStockRepository;
import com.alphadraft.skeleton.repository.StockRepository;
import com.alphadraft.skeleton.service.StockService;
import com.alphadraft.skeleton.validation.AllowedSectorRule;
import com.alphadraft.skeleton.validation.RequiredFieldsRule;
import com.alphadraft.skeleton.validation.StockValidationException;
import com.alphadraft.skeleton.validation.StockValidationRule;
import com.alphadraft.skeleton.validation.UniqueTickerRule;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SkeletonApplicationTests {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private List<StockValidationRule> validationRules;

    @Test
    void springInjectsInMemoryRepositoryIntoService() {
        assertInstanceOf(InMemoryStockRepository.class, stockRepository);
        assertEquals(6, stockService.getAllStocks().size());
    }

    @Test
    void springProvidesAllValidationRulesInOrder() {
        assertEquals(
                List.of(RequiredFieldsRule.class, AllowedSectorRule.class, UniqueTickerRule.class),
                validationRules.stream().map(Object::getClass).toList());
    }

    @Test
    void serviceRunsTheRealRulesBeforeCreating() {
        StockValidationException e = assertThrows(StockValidationException.class,
                () -> stockService.createStock(new StockRequest("aapl", "", "Crypto")));

        assertEquals(Map.of(
                "ticker", "Ticker already exists.",
                "name", "Company name is required.",
                "sector", "Choose a sector from the list."), e.getErrors());
        assertEquals(6, stockService.getAllStocks().size());
    }
}
