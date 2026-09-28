package com.alphadraft.skeleton.controller;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.service.StockService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/stocks")
    public List<Stock> getStocks() {
        return stockService.getAllStocks();
    }

    @GetMapping("/stocks/{id}")
    public ResponseEntity<Stock> getStock(@PathVariable int id) {
        return ResponseEntity.of(stockService.getStockById(id));
    }
}
