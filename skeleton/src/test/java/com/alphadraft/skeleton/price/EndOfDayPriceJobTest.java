package com.alphadraft.skeleton.price;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.DailyPrice;
import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.DailyPriceRepository;
import com.alphadraft.skeleton.testsupport.FakeStockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EndOfDayPriceJobTest {

    private static final LocalDate FRIDAY = LocalDate.of(2026, 11, 13);

    private static class FakePriceRepository implements DailyPriceRepository {
        final List<DailyPrice> saved = new ArrayList<>();

        @Override
        public boolean exists(int stockId, LocalDate tradeDate) {
            return saved.stream().anyMatch(p -> p.getStockId() == stockId && p.getTradeDate().equals(tradeDate));
        }

        @Override
        public DailyPrice save(DailyPrice price) {
            saved.add(price);
            return price;
        }

        @Override
        public List<DailyPrice> findLatestForEachStock() {
            return List.copyOf(saved);
        }
    }

    // Returns a fixed close per ticker; tickers missing from the map have no price, and tickers in
    // "broken" throw like a network failure would.
    private static class FakeProvider implements ClosingPriceProvider {
        final Map<String, ClosingPrice> closes = new HashMap<>();
        final Set<String> broken = new java.util.HashSet<>();
        boolean configured = true;

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public Optional<ClosingPrice> latestClose(String ticker) {
            if (broken.contains(ticker)) {
                throw new IllegalStateException("network down");
            }
            return Optional.ofNullable(closes.get(ticker));
        }
    }

    private static ClosingPrice close(LocalDate date, String price) {
        return new ClosingPrice(date, new BigDecimal(price), "twelvedata");
    }

    private static EndOfDayPriceJob job(FakeStockRepository stocks, FakePriceRepository prices, FakeProvider provider) {
        return new EndOfDayPriceJob(stocks, prices, provider, 0);
    }

    @Test
    void storesTheClosingPriceOfEveryStock() {
        FakeStockRepository stocks = new FakeStockRepository(
                new Stock(1, "AAPL", "Apple Inc.", "Technology"),
                new Stock(2, "JPM", "JPMorgan Chase", "Financials"));
        FakePriceRepository prices = new FakePriceRepository();
        FakeProvider provider = new FakeProvider();
        provider.closes.put("AAPL", close(FRIDAY, "230.5"));
        provider.closes.put("JPM", close(FRIDAY, "310.25"));

        int stored = job(stocks, prices, provider).recordClosingPrices();

        assertEquals(2, stored);
        assertEquals(2, prices.saved.size());
        DailyPrice aapl = prices.saved.get(0);
        assertEquals(1, aapl.getStockId());
        assertEquals(FRIDAY, aapl.getTradeDate());
        assertEquals(new BigDecimal("230.5000"), aapl.getClosePrice());
        assertEquals("twelvedata", aapl.getSource());
    }

    @Test
    void runningTwiceOnTheSameDayStoresEachPriceOnce() {
        FakeStockRepository stocks = new FakeStockRepository(new Stock(1, "AAPL", "Apple Inc.", "Technology"));
        FakePriceRepository prices = new FakePriceRepository();
        FakeProvider provider = new FakeProvider();
        provider.closes.put("AAPL", close(FRIDAY, "230.5"));
        EndOfDayPriceJob job = job(stocks, prices, provider);

        job.recordClosingPrices();
        int secondRun = job.recordClosingPrices();

        assertEquals(0, secondRun);
        assertEquals(1, prices.saved.size());
    }

    @Test
    void aStockWithNoPriceIsSkippedWithoutStoppingTheRun() {
        FakeStockRepository stocks = new FakeStockRepository(
                new Stock(1, "NOPE", "Unknown Co", "Technology"),
                new Stock(2, "JPM", "JPMorgan Chase", "Financials"));
        FakePriceRepository prices = new FakePriceRepository();
        FakeProvider provider = new FakeProvider();
        provider.closes.put("JPM", close(FRIDAY, "310.25"));

        assertEquals(1, job(stocks, prices, provider).recordClosingPrices());
        assertEquals(1, prices.saved.size());
        assertEquals(2, prices.saved.get(0).getStockId());
    }

    @Test
    void aProviderErrorForOneStockDoesNotStopTheOthers() {
        FakeStockRepository stocks = new FakeStockRepository(
                new Stock(1, "AAPL", "Apple Inc.", "Technology"),
                new Stock(2, "JPM", "JPMorgan Chase", "Financials"));
        FakePriceRepository prices = new FakePriceRepository();
        FakeProvider provider = new FakeProvider();
        provider.broken.add("AAPL");
        provider.closes.put("JPM", close(FRIDAY, "310.25"));

        assertEquals(1, job(stocks, prices, provider).recordClosingPrices());
        assertEquals(List.of(2), prices.saved.stream().map(DailyPrice::getStockId).toList());
    }

    @Test
    void doesNothingWhenNoApiKeyIsConfigured() {
        FakeStockRepository stocks = new FakeStockRepository(new Stock(1, "AAPL", "Apple Inc.", "Technology"));
        FakePriceRepository prices = new FakePriceRepository();
        FakeProvider provider = new FakeProvider();
        provider.configured = false;
        provider.closes.put("AAPL", close(FRIDAY, "230.5"));

        assertEquals(0, job(stocks, prices, provider).recordClosingPrices());
        assertTrue(prices.saved.isEmpty());
    }
}
