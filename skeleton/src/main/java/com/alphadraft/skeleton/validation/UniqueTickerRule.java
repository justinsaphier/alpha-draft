package com.alphadraft.skeleton.validation;

import com.alphadraft.skeleton.model.StockRequest;
import com.alphadraft.skeleton.repository.StockRepository;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class UniqueTickerRule implements StockValidationRule {

    private final StockRepository stockRepository;

    public UniqueTickerRule(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public Map<String, String> validate(StockRequest stock, Integer idBeingUpdated) {
        // A missing ticker is reported by RequiredFieldsRule, not here.
        if (stock.ticker() == null || stock.ticker().isBlank()) {
            return Map.of();
        }
        // A stock keeping its own ticker during an update is not a duplicate.
        boolean taken = stockRepository.findAll().stream()
                .filter(existing -> idBeingUpdated == null || existing.getId() != idBeingUpdated)
                .anyMatch(existing -> existing.getTicker().equalsIgnoreCase(stock.ticker()));
        return taken ? Map.of("ticker", "Ticker already exists.") : Map.of();
    }
}
