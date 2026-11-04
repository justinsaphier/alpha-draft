package com.alphadraft.skeleton.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.Stock;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditingStockRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");

    private final Stock aapl = new Stock(1, "AAPL", "Apple Inc.", "Technology");
    private final Stock jpm = new Stock(2, "JPM", "JPMorgan Chase & Co.", "Financials");

    private InMemoryStockRepository delegate;
    private AuditingStockRepository repository;

    @BeforeEach
    void setUp() {
        delegate = new InMemoryStockRepository();
        repository = new AuditingStockRepository(delegate, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void startsWithEmptyAuditLog() {
        assertTrue(repository.getAuditLog().isEmpty());
    }

    @Test
    void saveOfNewStockIsStoredAndRecordedAsCreate() {
        assertSame(aapl, repository.save(aapl));

        assertSame(aapl, delegate.findById(1).orElseThrow());
        assertEquals(List.of(new StockAuditEntry("CREATE", 1, "AAPL", NOW)), repository.getAuditLog());
    }

    @Test
    void saveOfExistingStockIsRecordedAsUpdate() {
        repository.save(aapl);
        repository.save(new Stock(1, "AAPL", "Apple", "Technology"));

        assertEquals("Apple", delegate.findById(1).orElseThrow().getName());
        assertEquals(List.of("CREATE", "UPDATE"),
                repository.getAuditLog().stream().map(StockAuditEntry::action).toList());
    }

    @Test
    void deleteRemovesStockAndRecordsItsTicker() {
        repository.save(aapl);

        assertTrue(repository.deleteById(1));

        assertTrue(delegate.findById(1).isEmpty());
        assertEquals(new StockAuditEntry("DELETE", 1, "AAPL", NOW), repository.getAuditLog().get(1));
    }

    @Test
    void deleteOfUnknownIdRecordsNothing() {
        assertFalse(repository.deleteById(99));

        assertTrue(repository.getAuditLog().isEmpty());
    }

    @Test
    void readsPassThroughWithoutBeingRecorded() {
        delegate.save(aapl);
        delegate.save(jpm);

        assertEquals(List.of(aapl, jpm), repository.findAll());
        assertSame(jpm, repository.findById(2).orElseThrow());
        assertTrue(repository.getAuditLog().isEmpty());
    }

    @Test
    void auditLogCannotBeEditedByCallers() {
        repository.save(aapl);

        assertThrows(UnsupportedOperationException.class, () -> repository.getAuditLog().clear());
        assertEquals(1, repository.getAuditLog().size());
    }
}
