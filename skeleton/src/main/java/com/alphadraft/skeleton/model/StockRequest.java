package com.alphadraft.skeleton.model;

// Request body for creating or updating a stock; the id comes from the server or the URL.
public record StockRequest(String ticker, String name, String sector) {

    // Trims every field and uppercases the ticker so rules see clean values.
    public StockRequest normalized() {
        String cleanTicker = trimmed(ticker);
        return new StockRequest(
                cleanTicker == null ? null : cleanTicker.toUpperCase(),
                trimmed(name),
                trimmed(sector));
    }

    // Extracted so each field's null handling is written once; null stays null.
    private static String trimmed(String value) {
        return value == null ? null : value.trim();
    }
}
