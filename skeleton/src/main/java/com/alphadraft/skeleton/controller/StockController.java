package com.alphadraft.skeleton.controller;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.model.StockRequest;
import com.alphadraft.skeleton.service.StockService;
import com.alphadraft.skeleton.validation.StockValidationException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/stocks")
    public ResponseEntity<Stock> createStock(@RequestBody StockRequest request) {
        Stock created = stockService.createStock(request);
        return ResponseEntity.created(URI.create("/stocks/" + created.getId())).body(created);
    }

    @PutMapping("/stocks/{id}")
    public ResponseEntity<Stock> updateStock(@PathVariable int id, @RequestBody StockRequest request) {
        return ResponseEntity.of(stockService.updateStock(id, request));
    }

    @DeleteMapping("/stocks/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable int id) {
        return stockService.deleteStock(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // 400 with { "errors": { field: message } }, the shape the frontend's stockService reads.
    @ExceptionHandler(StockValidationException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleValidation(StockValidationException e) {
        return errors(e.getErrors());
    }

    // A missing or malformed JSON body gets the same 400 shape instead of Spring's default.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleUnreadableBody(HttpMessageNotReadableException e) {
        return errors(Map.of("body", "Request body must be a JSON stock with ticker, name and sector."));
    }

    private static ResponseEntity<Map<String, Map<String, String>>> errors(Map<String, String> errors) {
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }
}
