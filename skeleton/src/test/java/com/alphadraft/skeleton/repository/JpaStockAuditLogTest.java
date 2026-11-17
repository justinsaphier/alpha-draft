package com.alphadraft.skeleton.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.Stock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

// Runs the audit log against a real (in-memory H2) database to show entries are stored in the
// stock_audit_log table and read back in order, not kept in a Java list.
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:audit-log-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class JpaStockAuditLogTest {

    private static final Instant T1 = Instant.parse("2026-11-17T12:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-17T12:05:00Z");

    @Autowired
    private StockAuditJpaRepository jpa;

    @Autowired
    private StockJpaRepository stockJpa;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void startsEmpty() {
        assertTrue(new JpaStockAuditLog(jpa).findAll().isEmpty());
    }

    @Test
    void entriesAreStoredInTheTableAndReadBackOldestFirst() {
        JpaStockAuditLog log = new JpaStockAuditLog(jpa);
        log.add(new StockAuditEntry("CREATE", 1, "AAPL", T1));
        log.add(new StockAuditEntry("DELETE", 1, "AAPL", T2));
        entityManager.flush();
        entityManager.clear();

        // A fresh log over the same table sees both entries, as the app would after a restart.
        assertEquals(List.of(
                new StockAuditEntry("CREATE", 1, "AAPL", T1),
                new StockAuditEntry("DELETE", 1, "AAPL", T2)), new JpaStockAuditLog(jpa).findAll());
        assertEquals(2, jpa.count());
    }

    @Test
    void decoratorOverRealTablesRecordsEachChange() {
        AuditingStockRepository repository = new AuditingStockRepository(
                new JpaStockRepository(stockJpa), new JpaStockAuditLog(jpa));

        Stock saved = repository.save(new Stock(0, "NVDA", "NVIDIA Corporation", "Technology"));
        repository.save(new Stock(saved.getId(), "NVDA", "NVIDIA", "Technology"));
        repository.deleteById(saved.getId());
        entityManager.flush();
        entityManager.clear();

        List<StockAuditEntry> entries = new JpaStockAuditLog(jpa).findAll();
        assertEquals(List.of("CREATE", "UPDATE", "DELETE"),
                entries.stream().map(StockAuditEntry::action).toList());
        assertTrue(entries.stream().allMatch(e -> e.stockId() == saved.getId() && "NVDA".equals(e.ticker())));
    }
}
