package com.alphadraft.skeleton.model;

// Request body for creating or updating a stock; the id comes from the URL or the server.
public record StockRequest(String ticker, String name, String sector) {
}
