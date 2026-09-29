package com.alphadraft.backend.controller;

import com.alphadraft.backend.model.Stock;
import com.alphadraft.backend.service.StockService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
}
