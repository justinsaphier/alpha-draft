package com.alphadraft.skeleton.model;

// Request body for creating or updating a stock; the id comes from the server or the URL.
public record StockRequest(String ticker, String name, String sector) {

    // Trims every field and uppercases the ticker so rules see clean values.
    public StockRequest normalized() {
        return new StockRequest(
                ticker == null ? null : ticker.trim().toUpperCase(),
                name == null ? null : name.trim(),
                sector == null ? null : sector.trim());
    }
}
