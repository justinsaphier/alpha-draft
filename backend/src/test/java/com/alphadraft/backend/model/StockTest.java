package com.alphadraft.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StockTest {

    @Test
    void gettersReturnConstructorValues() {
        Stock stock = new Stock(1, "AAPL", "Apple Inc.", "Technology");

        assertThat(stock.getId()).isEqualTo(1);
        assertThat(stock.getTicker()).isEqualTo("AAPL");
        assertThat(stock.getName()).isEqualTo("Apple Inc.");
        assertThat(stock.getSector()).isEqualTo("Technology");
    }
}
