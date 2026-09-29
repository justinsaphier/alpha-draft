package com.alphadraft.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.alphadraft.backend.model.Stock;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockServiceTest {

    private final StockService stockService = new StockService();

    @Test
    void getAllStocksReturnsSixStocks() {
        assertThat(stockService.getAllStocks()).hasSize(6);
    }

    @Test
    void getAllStocksHasUniqueIdsAndTickers() {
        List<Stock> stocks = stockService.getAllStocks();

        assertThat(stocks).extracting(Stock::getId).doesNotHaveDuplicates();
        assertThat(stocks).extracting(Stock::getTicker).doesNotHaveDuplicates();
    }

    @Test
    void getAllStocksContainsApple() {
        assertThat(stockService.getAllStocks())
                .anySatisfy(stock -> {
                    assertThat(stock.getTicker()).isEqualTo("AAPL");
                    assertThat(stock.getSector()).isEqualTo("Technology");
                });
    }

    @Test
    void getStockByIdReturnsMatchingStock() {
        assertThat(stockService.getStockById(3))
                .hasValueSatisfying(stock -> assertThat(stock.getTicker()).isEqualTo("JNJ"));
    }

    @Test
    void getStockByIdReturnsEmptyForUnknownId() {
        assertThat(stockService.getStockById(999)).isEmpty();
    }

    @Test
    void getAllStocksReturnsStocksInIdOrder() {
        assertThat(stockService.getAllStocks()).extracting(Stock::getId).isSorted();
    }
}
