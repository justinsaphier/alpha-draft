package com.alphadraft.skeleton.repository;

import com.alphadraft.skeleton.model.DailyPrice;
import java.time.LocalDate;
import java.util.List;

// Storage abstraction for daily closing prices. EndOfDayPriceJob depends on this interface, not
// on JPA, so the job can be unit tested without a database.
public interface DailyPriceRepository {

    // True if a closing price is already stored for this stock on this trading day.
    boolean exists(int stockId, LocalDate tradeDate);

    DailyPrice save(DailyPrice price);

    // The most recent stored closing price for each stock that has one.
    List<DailyPrice> findLatestForEachStock();
}
