package com.alphadraft.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alphadraft.backend.model.Stock;
import com.alphadraft.backend.service.StockService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StockController.class)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockService stockService;

    @Test
    void getStocksReturnsJsonList() throws Exception {
        when(stockService.getAllStocks()).thenReturn(List.of(
                new Stock(1, "AAPL", "Apple Inc.", "Technology"),
                new Stock(2, "JPM", "JPMorgan Chase & Co.", "Financials")));

        mockMvc.perform(get("/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[0].name").value("Apple Inc."))
                .andExpect(jsonPath("$[0].sector").value("Technology"))
                .andExpect(jsonPath("$[1].ticker").value("JPM"));
    }

    @Test
    void getStocksReturnsEmptyListWhenNoStocks() throws Exception {
        when(stockService.getAllStocks()).thenReturn(List.of());

        mockMvc.perform(get("/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getStockByIdReturnsStock() throws Exception {
        when(stockService.getStockById(1))
                .thenReturn(Optional.of(new Stock(1, "AAPL", "Apple Inc.", "Technology")));

        mockMvc.perform(get("/stocks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ticker").value("AAPL"));
    }

    @Test
    void getStockByIdReturns404WhenMissing() throws Exception {
        when(stockService.getStockById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/stocks/999"))
                .andExpect(status().isNotFound());
    }
}
