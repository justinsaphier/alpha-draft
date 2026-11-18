package com.alphadraft.skeleton.price;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.DailyPrice;
import com.alphadraft.skeleton.price.PriceRefreshService.StartOutcome;
import com.alphadraft.skeleton.repository.DailyPriceRepository;
import com.alphadraft.skeleton.repository.StockRepository;
import com.alphadraft.skeleton.testsupport.FakeStockRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

class PriceRefreshServiceTest {

    // Holds submitted tasks so a test decides when the background run happens.
    private static class ManualExecutor implements Executor {
        final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        void runAll() {
            List.copyOf(tasks).forEach(Runnable::run);
            tasks.clear();
        }
    }

    private static class NoopPrices implements DailyPriceRepository {
        @Override
        public boolean exists(int stockId, LocalDate tradeDate) {
            return false;
        }

        @Override
        public DailyPrice save(DailyPrice price) {
            return price;
        }

        @Override
        public List<DailyPrice> findLatestForEachStock() {
            return List.of();
        }
    }

    private static class FixedProvider implements ClosingPriceProvider {
        final boolean configured;
        RuntimeException failure;

        FixedProvider(boolean configured) {
            this.configured = configured;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public Optional<ClosingPrice> latestClose(String ticker) {
            if (failure != null) {
                throw failure;
            }
            return Optional.empty();
        }
    }

    private static PriceRefreshService service(StockRepository stocks, FixedProvider provider, Executor executor) {
        EndOfDayPriceJob job = new EndOfDayPriceJob(stocks, new NoopPrices(), provider, 0);
        return new PriceRefreshService(job, provider, executor);
    }

    @Test
    void refusesToStartWithoutAnApiKey() {
        ManualExecutor executor = new ManualExecutor();
        PriceRefreshService refresh = service(new FakeStockRepository(), new FixedProvider(false), executor);

        assertEquals(StartOutcome.NOT_CONFIGURED, refresh.start());
        assertTrue(executor.tasks.isEmpty());
        assertFalse(refresh.status().running());
        assertFalse(refresh.status().configured());
    }

    @Test
    void startsOneRunAndReportsItAsRunningUntilItFinishes() {
        ManualExecutor executor = new ManualExecutor();
        PriceRefreshService refresh = service(new FakeStockRepository(), new FixedProvider(true), executor);

        assertEquals(StartOutcome.STARTED, refresh.start());
        assertTrue(refresh.status().running());
        assertNotNull(refresh.status().lastStartedAt());
        assertNull(refresh.status().lastFinishedAt());
    }

    @Test
    void secondStartWhileRunningIsRefused() {
        ManualExecutor executor = new ManualExecutor();
        PriceRefreshService refresh = service(new FakeStockRepository(), new FixedProvider(true), executor);

        refresh.start();

        assertEquals(StartOutcome.ALREADY_RUNNING, refresh.start());
        assertEquals(1, executor.tasks.size());
    }

    @Test
    void finishedRunRecordsHowManyPricesWereStoredAndCanBeStartedAgain() {
        ManualExecutor executor = new ManualExecutor();
        PriceRefreshService refresh = service(new FakeStockRepository(), new FixedProvider(true), executor);

        refresh.start();
        executor.runAll();

        PriceRefreshStatus status = refresh.status();
        assertFalse(status.running());
        assertNotNull(status.lastFinishedAt());
        assertEquals(0, status.lastStored());
        assertNull(status.lastError());
        assertEquals(StartOutcome.STARTED, refresh.start());
    }

    @Test
    void aFailedRunIsReportedAndDoesNotBlockTheNextRun() {
        ManualExecutor executor = new ManualExecutor();
        FixedProvider provider = new FixedProvider(true);
        provider.failure = new IllegalStateException("Twelve Data is down");
        FakeStockRepository stocks = new FakeStockRepository(
                new com.alphadraft.skeleton.model.Stock(1, "AAPL", "Apple Inc.", "Technology"));
        PriceRefreshService refresh = service(stocks, provider, executor);

        refresh.start();
        // One stock failing is logged by the job and does not fail the run as a whole.
        executor.runAll();
        assertFalse(refresh.status().running());
        assertEquals(0, refresh.status().lastStored());

        // A whole-run failure (here, the store itself) is recorded as the last error.
        PriceRefreshService broken = new PriceRefreshService(
                new EndOfDayPriceJob(stocks, new NoopPrices(), provider, 0) {
                    @Override
                    public int recordClosingPrices() {
                        throw new IllegalStateException("database unavailable");
                    }
                }, provider, executor);
        broken.start();
        executor.runAll();
        assertEquals("database unavailable", broken.status().lastError());
        assertFalse(broken.status().running());
    }

    @Test
    void aRejectedTaskDoesNotLeaveTheServiceStuckAsRunning() {
        Executor rejecting = command -> {
            throw new java.util.concurrent.RejectedExecutionException("shut down");
        };
        PriceRefreshService refresh = service(new FakeStockRepository(), new FixedProvider(true), rejecting);

        assertThrows(java.util.concurrent.RejectedExecutionException.class, refresh::start);
        assertFalse(refresh.status().running());
    }
}
