package com.alphadraft.skeleton.price;

import java.util.Optional;

// Source of closing prices. EndOfDayPriceJob depends on this interface, so the market-data API
// can be swapped or faked without touching the job.
public interface ClosingPriceProvider {

    // False when no API key is configured; the job then skips the run instead of failing.
    boolean isConfigured();

    // The most recent closing price for the ticker, or empty if the provider has none.
    Optional<ClosingPrice> latestClose(String ticker);
}
