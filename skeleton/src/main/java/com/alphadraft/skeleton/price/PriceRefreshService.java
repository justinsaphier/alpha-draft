package com.alphadraft.skeleton.price;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// Runs EndOfDayPriceJob in the background, either on the weekday schedule (4:30 PM New York time)
// or when an admin asks for a refresh. A full S&P 500 run takes about ten minutes, so the caller
// gets an answer straight away and polls status() for progress. Only one run can be active at a time.
@Service
public class PriceRefreshService {

    public enum StartOutcome { STARTED, ALREADY_RUNNING, NOT_CONFIGURED }

    private static final Logger log = LoggerFactory.getLogger(PriceRefreshService.class);

    private final EndOfDayPriceJob job;
    private final ClosingPriceProvider provider;
    private final Executor executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile Instant lastStartedAt;
    private volatile Instant lastFinishedAt;
    private volatile Integer lastStored;
    private volatile String lastError;

    @Autowired
    public PriceRefreshService(EndOfDayPriceJob job, ClosingPriceProvider provider) {
        this(job, provider, Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "end-of-day-prices");
            thread.setDaemon(true);
            return thread;
        }));
    }

    PriceRefreshService(EndOfDayPriceJob job, ClosingPriceProvider provider, Executor executor) {
        this.job = job;
        this.provider = provider;
        this.executor = executor;
    }

    @Scheduled(cron = "${app.prices.cron:0 30 16 * * MON-FRI}", zone = "America/New_York")
    public void scheduledRefresh() {
        StartOutcome outcome = start();
        log.info("Scheduled end-of-day price refresh: {}", outcome);
    }

    public StartOutcome start() {
        if (!provider.isConfigured()) {
            return StartOutcome.NOT_CONFIGURED;
        }
        if (!running.compareAndSet(false, true)) {
            return StartOutcome.ALREADY_RUNNING;
        }
        lastStartedAt = Instant.now();
        lastError = null;
        try {
            executor.execute(this::runRefresh);
        } catch (RuntimeException e) {
            // The executor refused the task, so no run is active and the flag must not stay set.
            running.set(false);
            throw e;
        }
        return StartOutcome.STARTED;
    }

    public PriceRefreshStatus status() {
        return new PriceRefreshStatus(
                provider.isConfigured(),
                running.get(),
                lastStartedAt,
                lastFinishedAt,
                lastStored,
                lastError);
    }

    private void runRefresh() {
        try {
            lastStored = job.recordClosingPrices();
            log.info("End-of-day price refresh stored {} closing price(s)", lastStored);
        } catch (RuntimeException e) {
            lastError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.error("End-of-day price refresh failed", e);
        } finally {
            lastFinishedAt = Instant.now();
            running.set(false);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (executor instanceof ExecutorService service) {
            service.shutdownNow();
        }
    }
}
