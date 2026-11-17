package com.alphadraft.skeleton.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.repository.StockJpaRepository;
import com.alphadraft.skeleton.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

// Checks the JPA mapping of Stock against a schema Hibernate generates from the entities in
// an in-memory H2 database. Each test runs in a transaction that is rolled back.
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:stock-mapping-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class StockMappingTest {

    @Autowired
    private StockJpaRepository stocks;

    @Autowired
    private UserRepository users;

    @Autowired
    private EntityManager entityManager;

    // Writes pending changes and empties the persistence context, so the next read
    // really comes from the database instead of the cached object.
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void stockRoundTripsThroughTheStocksTable() {
        int id = stocks.saveAndFlush(new Stock(0, "AAPL", "Apple Inc.", "Technology")).getId();
        flushAndClear();

        Stock loaded = stocks.findById(id).orElseThrow();

        assertTrue(id > 0);
        assertEquals("AAPL", loaded.getTicker());
        assertEquals("Apple Inc.", loaded.getName());
        assertEquals("Technology", loaded.getSector());
    }

    @Test
    void databaseRejectsADuplicateTicker() {
        stocks.saveAndFlush(new Stock(0, "AAPL", "Apple Inc.", "Technology"));

        assertThrows(DataIntegrityViolationException.class,
                () -> stocks.saveAndFlush(new Stock(0, "AAPL", "Another Apple", "Technology")));
    }

    @Test
    void databaseRejectsAMissingTicker() {
        assertThrows(DataIntegrityViolationException.class,
                () -> stocks.saveAndFlush(new Stock(0, null, "No Ticker Inc.", "Technology")));
    }

    @Test
    void newStockStartsInThePoolWithNoOwner() {
        int id = stocks.saveAndFlush(new Stock(0, "AAPL", "Apple Inc.", "Technology")).getId();
        flushAndClear();

        assertNull(stocks.findById(id).orElseThrow().getDraftedBy());
    }

    @Test
    void draftedStockRemembersItsOwner() {
        User player = users.saveAndFlush(new User("player@example.com", "hash", Role.PLAYER));
        Stock stock = new Stock(0, "AAPL", "Apple Inc.", "Technology");
        stock.draftTo(player);
        int id = stocks.saveAndFlush(stock).getId();
        flushAndClear();

        assertEquals("player@example.com", stocks.findById(id).orElseThrow().getDraftedBy().getEmail());
    }

    @Test
    void oneUserCanOwnManyStocks() {
        User player = users.saveAndFlush(new User("player@example.com", "hash", Role.PLAYER));
        Stock aapl = new Stock(0, "AAPL", "Apple Inc.", "Technology");
        Stock jpm = new Stock(0, "JPM", "JPMorgan Chase & Co.", "Financials");
        aapl.draftTo(player);
        jpm.draftTo(player);
        int aaplId = stocks.saveAndFlush(aapl).getId();
        int jpmId = stocks.saveAndFlush(jpm).getId();
        flushAndClear();

        long ownerOfAapl = stocks.findById(aaplId).orElseThrow().getDraftedBy().getId();
        long ownerOfJpm = stocks.findById(jpmId).orElseThrow().getDraftedBy().getId();
        assertEquals(player.getId(), ownerOfAapl);
        assertEquals(player.getId(), ownerOfJpm);
    }

    @Test
    void returnToPoolClearsTheOwner() {
        User player = users.saveAndFlush(new User("player@example.com", "hash", Role.PLAYER));
        Stock stock = new Stock(0, "AAPL", "Apple Inc.", "Technology");
        stock.draftTo(player);
        int id = stocks.saveAndFlush(stock).getId();
        flushAndClear();

        Stock loaded = stocks.findById(id).orElseThrow();
        loaded.returnToPool();
        stocks.saveAndFlush(loaded);
        flushAndClear();

        assertNull(stocks.findById(id).orElseThrow().getDraftedBy());
    }
}