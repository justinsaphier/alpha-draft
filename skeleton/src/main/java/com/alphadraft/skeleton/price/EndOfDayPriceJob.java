package com.alphadraft.skeleton.price;

import com.alphadraft.skeleton.model.DailyPrice;
import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.DailyPriceRepository;
import com.alphadraft.skeleton.repository.StockRepository;
import java.math.RoundingMode;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Stores each stock's closing price once per trading day. PriceRefreshService decides when it runs
// (weekdays at 4:30 PM New York time, or when an admin asks). Twelve Data's free tier allows 8 calls a
// minute, so the job pauses between calls; a full run over the S&P 500 takes roughly ten minutes.
//
// Runs are idempotent: a stock/day pair that already has a price is skipped. A run on a holiday
// or weekend finds the previous trading day's close, which is already stored, so it adds nothing.
@Component
public class EndOfDayPriceJob {

    private static final Logger log = LoggerFactory.getLogger(EndOfDayPriceJob.class);

    private final StockRepository stocks;
    private final DailyPriceRepository prices;
    private final ClosingPriceProvider provider;
    private final long requestDelayMillis;

    public EndOfDayPriceJob(StockRepository stocks,
                            DailyPriceRepository prices,
                            ClosingPriceProvider provider,
                            @Value("${app.prices.request-delay-ms:8000}") long requestDelayMillis) {
        this.stocks = stocks;
        this.prices = prices;
        this.provider = provider;
        this.requestDelayMillis = requestDelayMillis;
    }

    // Returns how many new closing prices were stored.
    public int recordClosingPrices() {
        if (!provider.isConfigured()) {
            log.warn("TWELVE_DATA_API_KEY is not set; skipping the end-of-day price update.");
            return 0;
        }
        int stored = 0;
        int failed = 0;
        for (Stock stock : stocks.findAll()) {
            try {
                if (recordOne(stock)) {
                    stored++;
                }
            } catch (RuntimeException e) {
                // One bad ticker or a network blip must not stop the other stocks.
                failed++;
                log.warn("Could not record the closing price for {}: {}", stock.getTicker(), e.getMessage());
            }
        }
        if (failed > 0) {
            log.warn("End-of-day price update: {} stock(s) failed and can be retried by the next run", failed);
        }
        return stored;
    }

    private boolean recordOne(Stock stock) {
        Optional<ClosingPrice> latest;
        try {
            latest = provider.latestClose(stock.getTicker());
        } finally {
            pause();
        }
        if (latest.isEmpty()) {
            log.debug("No closing price available for {}", stock.getTicker());
            return false;
        }
        ClosingPrice close = latest.get();
        if (prices.exists(stock.getId(), close.tradeDate())) {
            return false;
        }
        prices.save(new DailyPrice(stock.getId(), close.tradeDate(),
                close.price().setScale(4, RoundingMode.HALF_UP), close.source()));
        return true;
    }

    private void pause() {
        if (requestDelayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(requestDelayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
