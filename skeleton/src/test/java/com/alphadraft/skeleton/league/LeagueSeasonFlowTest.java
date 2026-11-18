package com.alphadraft.skeleton.league;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.DailyPrice;
import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.DailyPriceJpaRepository;
import com.alphadraft.skeleton.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Runs a whole quarter against the real stock pool: join, accept, draft, lock, carry-over, scoring.
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:league-flow-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes-long",
    "app.jwt.expiration-minutes=60",
    "app.league.draft-check-ms=3600000"
})
class LeagueSeasonFlowTest {

    private static final String ALICE = "alice@league.test";
    private static final String BOB = "bob@league.test";

    @Autowired LeagueService league;
    @Autowired DraftService draft;
    @Autowired CallService calls;
    @Autowired SeasonService season;
    @Autowired TeamRepository teams;
    @Autowired DraftPickRepository picks;
    @Autowired StockRepository stocks;
    @Autowired DailyPriceJpaRepository dailyPrices;

    private void priceEveryStock(LocalDate date, String close) {
        List<DailyPrice> rows = stocks.findAll().stream()
                .map(s -> new DailyPrice(s.getId(), date, new BigDecimal(close), "twelvedata"))
                .toList();
        dailyPrices.saveAll(rows);
    }

    private void draftForEveryHuman() {
        while (league.league().getStatus() == LeagueStatus.DRAFTING) {
            DraftPick turn = picks.findFirstByStockIdIsNullOrderByOverallAsc().orElseThrow();
            Team team = teams.findById(turn.getTeamId()).orElseThrow();
            assertTrue(!team.isBot(), "bots should have picked already");
            Set<Integer> taken = new HashSet<>();
            for (DraftPick pick : picks.findAll()) {
                if (pick.getStockId() != null) {
                    taken.add(pick.getStockId());
                }
            }
            Stock choice = stocks.findAll().stream()
                    .filter(s -> RosterSpots.spotForSector(s.getSector()) == turn.getSpotId())
                    .filter(s -> !taken.contains(s.getId()))
                    .min((a, b) -> a.getTicker().compareTo(b.getTicker()))
                    .orElseThrow();
            draft.pick(team.getOwnerEmail(), choice.getTicker());
        }
    }

    private static java.util.Map<String, String> allLong(List<String> tickers) {
        java.util.Map<String, String> dirs = new java.util.HashMap<>();
        for (String t : tickers) {
            dirs.put(t, CallService.LONG);
        }
        return dirs;
    }

    @Test
    void aFullQuarterDraftsLocksCarriesOverAndScores() {
        priceEveryStock(LocalDate.of(2026, 11, 16), "100");
        league.join(ALICE, "Alice's Team");
        league.join(BOB, "Bob's Team");
        league.accept(teams.findByOwnerEmailIgnoreCase(ALICE).orElseThrow().getId());
        league.accept(teams.findByOwnerEmailIgnoreCase(BOB).orElseThrow().getId());
        league.updateSettings("Flow League", 8, null, "2026-12");

        draft.startDraft();
        assertEquals(LeagueStatus.DRAFTING, league.league().getStatus());
        assertEquals(8 * 6, picks.count());

        Team alice = teams.findByOwnerEmailIgnoreCase(ALICE).orElseThrow();
        Team bob = teams.findByOwnerEmailIgnoreCase(BOB).orElseThrow();
        // Whoever is not on the clock cannot pick out of turn.
        long onClock = picks.findFirstByStockIdIsNullOrderByOverallAsc().orElseThrow().getTeamId();
        String offClock = onClock == alice.getId() ? BOB : ALICE;
        assertThrows(LeagueException.class, () -> draft.pick(offClock, "AAPL"));

        draftForEveryHuman();
        assertEquals(LeagueStatus.PRESEASON, league.league().getStatus());

        // Pre-season: each human sets and submits month 0 calls before the season starts.
        List<String> aliceRoster = calls.rosterTickers(alice.getId());
        calls.save(ALICE, 0, allLong(aliceRoster), aliceRoster.get(0));
        calls.submit(ALICE, 0);
        assertThrows(LeagueException.class, () -> calls.save(ALICE, 0, allLong(aliceRoster), aliceRoster.get(0)));

        season.startSeason();
        assertEquals(LeagueStatus.IN_SEASON, league.league().getStatus());
        assertEquals(0, league.league().getCurrentMonth());

        // Month 0 is locked, so anyone can see Alice's calls; month 1 is still open, so Bob cannot yet.
        assertEquals(0, calls.viewFor(BOB, alice.getId(), 0).month());
        assertThrows(LeagueException.class, () -> calls.viewFor(BOB, alice.getId(), 1));

        // Alice sets month 1 (upcoming) and submits it; Bob does not, so he is carried over.
        calls.save(ALICE, 1, allLong(aliceRoster), aliceRoster.get(0));
        calls.submit(ALICE, 1);

        // Month 0 closes at 110 (+10%): every long call gains 10, the 2x call gains 20.
        priceEveryStock(LocalDate.of(2026, 12, 31), "110");
        season.finalizeMonth();
        assertEquals(1, league.league().getCurrentMonth());

        SeasonService.StandingRow aliceRow = season.standings().stream()
                .filter(r -> r.teamId() == alice.getId()).findFirst().orElseThrow();
        assertEquals(70.0, aliceRow.monthly().get(0), 1e-6);

        // Month 1 closes at 99 (-10%): the same long book now loses 70.
        priceEveryStock(LocalDate.of(2027, 1, 31), "99");
        season.finalizeMonth();
        // Month 2 stays flat at 99 and finishes the season.
        priceEveryStock(LocalDate.of(2027, 2, 28), "99");
        season.finalizeMonth();
        assertEquals(LeagueStatus.COMPLETE, league.league().getStatus());

        aliceRow = season.standings().stream().filter(r -> r.teamId() == alice.getId()).findFirst().orElseThrow();
        assertEquals(70.0, aliceRow.monthly().get(0), 1e-6);
        assertEquals(-70.0, aliceRow.monthly().get(1), 1e-6);
        assertEquals(0.0, aliceRow.monthly().get(2), 1e-6);
        assertEquals(0.0, aliceRow.total(), 1e-6);
    }
}
