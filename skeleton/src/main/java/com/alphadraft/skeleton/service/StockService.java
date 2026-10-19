package com.alphadraft.skeleton.service;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.StockRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    // Depends on the StockRepository interface (Dependency Inversion), so storage
    // can be swapped, e.g. for a database or a fake in tests, without changing this class.
    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    public Optional<Stock> getStockById(int id) {
        return stockRepository.findById(id);
    }
}
