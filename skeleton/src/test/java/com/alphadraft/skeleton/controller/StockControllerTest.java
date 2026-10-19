package com.alphadraft.skeleton.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.model.StockRequest;
import com.alphadraft.skeleton.service.StockService;
import com.alphadraft.skeleton.validation.StockValidationException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StockController.class)
class StockControllerTest {

    private static final String NVDA_JSON =
            "{\"ticker\":\"NVDA\",\"name\":\"NVIDIA Corporation\",\"sector\":\"Technology\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockService stockService;

    @Test
    void postCreatesStockAndReturns201() throws Exception {
        when(stockService.createStock(any(StockRequest.class)))
                .thenReturn(new Stock(7, "NVDA", "NVIDIA Corporation", "Technology"));

        mockMvc.perform(post("/stocks").contentType(MediaType.APPLICATION_JSON).content(NVDA_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/stocks/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.ticker").value("NVDA"));
    }

    @Test
    void postReturns400WithFieldErrorsWhenValidationFails() throws Exception {
        when(stockService.createStock(any(StockRequest.class)))
                .thenThrow(new StockValidationException(Map.of("ticker", "Ticker already exists.")));

        mockMvc.perform(post("/stocks").contentType(MediaType.APPLICATION_JSON).content(NVDA_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.ticker").value("Ticker already exists."));
    }

    @Test
    void putUpdatesStock() throws Exception {
        when(stockService.updateStock(eq(1), any(StockRequest.class)))
                .thenReturn(Optional.of(new Stock(1, "NVDA", "NVIDIA Corporation", "Technology")));

        mockMvc.perform(put("/stocks/1").contentType(MediaType.APPLICATION_JSON).content(NVDA_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ticker").value("NVDA"));
    }

    @Test
    void putReturns404ForUnknownId() throws Exception {
        when(stockService.updateStock(eq(99), any(StockRequest.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/stocks/99").contentType(MediaType.APPLICATION_JSON).content(NVDA_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void putReturns400WithFieldErrorsWhenValidationFails() throws Exception {
        when(stockService.updateStock(eq(1), any(StockRequest.class)))
                .thenThrow(new StockValidationException(Map.of("sector", "Choose a sector from the list.")));

        mockMvc.perform(put("/stocks/1").contentType(MediaType.APPLICATION_JSON).content(NVDA_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sector").value("Choose a sector from the list."));
    }
}
