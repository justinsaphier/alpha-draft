package com.alphadraft.skeleton.league;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.DailyPrice;
import com.alphadraft.skeleton.repository.DailyPriceJpaRepository;
import com.alphadraft.skeleton.repository.StockRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// The month closes by itself once it has ended and every drafted stock has a current price,
// and the player's live points come from today's closes.
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:auto-close-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes-long",
    "app.jwt.expiration-minutes=60",
    "app.league.draft-check-ms=3600000",
    "app.league.pick-check-ms=3600000"
})
class SeasonAutoCloseTest {

    private static final String PLAYER = "auto-close-player@league.test";

    @Autowired LeagueService league;
    @Autowired DraftService draft;
    @Autowired CallService calls;
    @Autowired SeasonService season;
    @Autowired TeamRepository teams;
    @Autowired DraftPickRepository picks;
    @Autowired StockRepository stocks;
    @Autowired DailyPriceJpaRepository dailyPrices;

    private void priceEveryStock(LocalDate date, String close) {
        dailyPrices.saveAll(stocks.findAll().stream()
                .map(s -> new DailyPrice(s.getId(), date, new BigDecimal(close), "twelvedata"))
                .toList());
    }

    @Test
    void theMonthClosesOnceItHasEndedAndPricesAreCurrent() {
        league.join(PLAYER, "Auto Team");
        league.accept(teams.findByOwnerEmailIgnoreCase(PLAYER).orElseThrow().getId());
        league.updateSettings("Auto League", 6, null, "2026-12", 30);
        draft.startDraft();
        // With one player, every pick is theirs; let the timer make each one.
        while (league.league().getStatus() == LeagueStatus.DRAFTING) {
            DraftPick turn = picks.findFirstByStockIdIsNullOrderByOverallAsc().orElseThrow();
            turn.setDeadline(Instant.now().minusSeconds(1));
            picks.save(turn);
            draft.autoPickExpired();
        }
        var team = teams.findByOwnerEmailIgnoreCase(PLAYER).orElseThrow();
        assertEquals(6, calls.rosterTickers(team.getId()).size());

        // Season starts on the first of December with the starting prices in place.
        priceEveryStock(LocalDate.of(2026, 11, 16), "100");
        season.startSeason(); // the first month locks: the player's default calls go in automatically
        assertEquals(0, league.league().getCurrentMonth());

        // A price dated inside the month: live points are visible straight away.
        priceEveryStock(LocalDate.of(2026, 12, 20), "110");
        double livePoints = season.liveBreakdown(team.getId()).stream().mapToDouble(SeasonService.LiveRow::points).sum();
        assertEquals(70.0, livePoints, 1e-6);

        // Still December: the month is not over, so nothing closes.
        assertFalse(season.finalizeIfDue(LocalDate.of(2026, 12, 15)));

        // Month over, but the newest prices are from before the month's last few days: wait for fresh prices.
        LocalDate staleDay = LocalDate.of(2027, 1, 2);
        priceEveryStock(LocalDate.of(2026, 11, 20), "100");
        assertFalse(season.finalizeIfDue(staleDay), "stale prices do not close the month");
        assertEquals(0, league.league().getCurrentMonth());

        // Fresh closes at month end: the month closes and the next one goes live.
        priceEveryStock(LocalDate.of(2026, 12, 31), "110");
        assertTrue(season.finalizeIfDue(staleDay));
        assertEquals(1, league.league().getCurrentMonth());
    }
}
