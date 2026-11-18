package com.alphadraft.skeleton.price;

import java.math.BigDecimal;
import java.time.LocalDate;

// The closing price a provider reported for one trading day.
public record ClosingPrice(LocalDate tradeDate, BigDecimal price, String source) {
}
