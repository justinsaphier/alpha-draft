package com.alphadraft.skeleton.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.model.StockRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockServiceTest {

    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockService();
    }

    @Test
    void getAllStocks_returnsSeededStocksInIdOrder() {
        // Arrange: service is created with 6 seeded stocks

        // Act
        List<Stock> stocks = stockService.getAllStocks();

        // Assert
        assertThat(stocks).hasSize(6);
        assertThat(stocks).extracting(Stock::getId).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(stocks.get(0).getTicker()).isEqualTo("AAPL");
    }

    @Test
    void getStockById_existingId_returnsStock() {
        // Arrange
        int id = 3;

        // Act
        Optional<Stock> result = stockService.getStockById(id);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getTicker()).isEqualTo("JNJ");
        assertThat(result.get().getName()).isEqualTo("Johnson & Johnson");
    }

    @Test
    void getStockById_missingId_returnsEmpty() {
        // Arrange
        int missingId = 99;

        // Act
        Optional<Stock> result = stockService.getStockById(missingId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void createStock_assignsNextIdAndStoresStock() {
        // Arrange
        StockRequest request = new StockRequest("MSFT", "Microsoft", "Technology");

        // Act
        Stock created = stockService.createStock(request);

        // Assert
        assertThat(created.getId()).isEqualTo(7);
        assertThat(created.getTicker()).isEqualTo("MSFT");
        assertThat(created.getName()).isEqualTo("Microsoft");
        assertThat(created.getSector()).isEqualTo("Technology");
        assertThat(stockService.getStockById(7)).contains(created);
        assertThat(stockService.getAllStocks()).hasSize(7);
    }

    @Test
    void createStock_afterDelete_doesNotReuseId() {
        // Arrange
        stockService.deleteStock(6);

        // Act
        Stock created = stockService.createStock(new StockRequest("MSFT", "Microsoft", "Technology"));

        // Assert
        assertThat(created.getId()).isEqualTo(7);
    }

    @Test
    void updateStock_existingId_replacesFieldsAndKeepsId() {
        // Arrange
        StockRequest request = new StockRequest("AAPL", "Apple Incorporated", "Consumer Tech");

        // Act
        Optional<Stock> result = stockService.updateStock(1, request);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1);
        assertThat(result.get().getName()).isEqualTo("Apple Incorporated");
        assertThat(result.get().getSector()).isEqualTo("Consumer Tech");
        assertThat(stockService.getStockById(1)).contains(result.get());
    }

    @Test
    void updateStock_missingId_returnsEmptyAndDoesNotCreate() {
        // Arrange
        StockRequest request = new StockRequest("X", "Missing", "None");

        // Act
        Optional<Stock> result = stockService.updateStock(99, request);

        // Assert
        assertThat(result).isEmpty();
        assertThat(stockService.getStockById(99)).isEmpty();
        assertThat(stockService.getAllStocks()).hasSize(6);
    }

    @Test
    void deleteStock_existingId_removesStockAndReturnsTrue() {
        // Arrange
        int id = 2;

        // Act
        boolean deleted = stockService.deleteStock(id);

        // Assert
        assertThat(deleted).isTrue();
        assertThat(stockService.getStockById(id)).isEmpty();
        assertThat(stockService.getAllStocks()).hasSize(5);
    }

    @Test
    void deleteStock_missingId_returnsFalse() {
        // Arrange
        int missingId = 99;

        // Act
        boolean deleted = stockService.deleteStock(missingId);

        // Assert
        assertThat(deleted).isFalse();
        assertThat(stockService.getAllStocks()).hasSize(6);
    }
}
