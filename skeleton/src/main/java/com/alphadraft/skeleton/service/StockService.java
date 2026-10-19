package com.alphadraft.skeleton.service;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.model.StockRequest;
import com.alphadraft.skeleton.repository.StockRepository;
import com.alphadraft.skeleton.validation.StockValidationException;
import com.alphadraft.skeleton.validation.StockValidationRule;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    // Depends on the StockRepository interface (Dependency Inversion), so storage
    // can be swapped, e.g. for a database or a fake in tests, without changing this class.
    private final StockRepository stockRepository;

    // Strategy pattern: Spring injects every StockValidationRule bean, in @Order order.
    private final List<StockValidationRule> validationRules;

    public StockService(StockRepository stockRepository, List<StockValidationRule> validationRules) {
        this.stockRepository = stockRepository;
        this.validationRules = validationRules;
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    public Optional<Stock> getStockById(int id) {
        return stockRepository.findById(id);
    }

    public synchronized Stock createStock(StockRequest request) {
        StockRequest stock = request.normalized();
        validate(stock, null);
        return stockRepository.save(new Stock(nextId(), stock.ticker(), stock.name(), stock.sector()));
    }

    public synchronized Optional<Stock> updateStock(int id, StockRequest request) {
        if (stockRepository.findById(id).isEmpty()) {
            return Optional.empty();
        }
        StockRequest stock = request.normalized();
        validate(stock, id);
        return Optional.of(stockRepository.save(new Stock(id, stock.ticker(), stock.name(), stock.sector())));
    }

    // Runs every rule and keeps the first error reported for each field.
    private void validate(StockRequest stock, Integer idBeingUpdated) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (StockValidationRule rule : validationRules) {
            rule.validate(stock, idBeingUpdated).forEach(errors::putIfAbsent);
        }
        if (!errors.isEmpty()) {
            throw new StockValidationException(errors);
        }
    }

    private int nextId() {
        return stockRepository.findAll().stream().mapToInt(Stock::getId).max().orElse(0) + 1;
    }
}
