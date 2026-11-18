package com.alphadraft.skeleton.league;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.StockJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

// Checks that the league's foreign keys are real JPA relationships: a row loaded from the database
// reaches its team and its stock through the mapped association.
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:league-relationships-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class LeagueRelationshipsTest {

    @Autowired TeamRepository teams;
    @Autowired DraftPickRepository picks;
    @Autowired CallSheetRepository sheets;
    @Autowired StockJpaRepository stocks;
    @Autowired EntityManager entities;

    @Test
    void aDraftPickReachesItsTeamAndItsStock() {
        Team team = teams.save(new Team("Relationship Team", "#123456", "rel@league.test",
                TeamStatus.ACCEPTED, false, Instant.now()));
        Stock stock = stocks.save(new Stock(0, "RELX", "Relationship Corp", "Technology"));
        DraftPick pick = picks.save(new DraftPick(1, 1, team.getId(), 1));
        pick.make(stock.getId(), stock.getTicker(), Instant.now());
        picks.save(pick);

        entities.flush();
        entities.clear();

        DraftPick loaded = picks.findById(pick.getId()).orElseThrow();
        assertEquals("Relationship Team", loaded.getTeam().getName());
        assertEquals("RELX", loaded.getStock().getTicker());
    }

    @Test
    void aCallSheetReachesItsTeam() {
        Team team = teams.save(new Team("Sheet Team", "#654321", "sheet@league.test",
                TeamStatus.ACCEPTED, false, Instant.now()));
        CallSheet sheet = sheets.save(new CallSheet(team.getId(), 0, true, "Submitted"));

        entities.flush();
        entities.clear();

        assertEquals("Sheet Team", sheets.findById(sheet.getId()).orElseThrow().getTeam().getName());
    }
}
