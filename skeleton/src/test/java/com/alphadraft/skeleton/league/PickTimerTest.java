package com.alphadraft.skeleton.league;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// A player's turn has a timer. When it runs out the league picks for them and the draft moves on.
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:pick-timer-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes-long",
    "app.jwt.expiration-minutes=60",
    "app.league.draft-check-ms=3600000",
    "app.league.pick-check-ms=3600000"
})
class PickTimerTest {

    private static final String PLAYER = "timer-player@league.test";

    @Autowired LeagueService league;
    @Autowired DraftService draft;
    @Autowired DraftPickRepository picks;
    @Autowired TeamRepository teams;

    @Test
    void aPickTimeOutsideTheRangeIsRefused() {
        assertThrows(LeagueException.class, () -> league.updateSettings("Timer", 6, null, "2026-12", 10));
        assertThrows(LeagueException.class, () -> league.updateSettings("Timer", 6, null, "2026-12", 900));
    }

    @Test
    void anExpiredTurnIsPickedForThePlayerAndTheClockMovesOn() {
        league.join(PLAYER, "Timer Team");
        league.accept(teams.findByOwnerEmailIgnoreCase(PLAYER).orElseThrow().getId());
        league.updateSettings("Timer League", 6, null, "2026-12", 30);
        draft.startDraft();

        DraftPick turn = picks.findFirstByStockIdIsNullOrderByOverallAsc().orElseThrow();
        assertNotNull(turn.getDeadline(), "a player's turn starts the timer");
        assertTrue(turn.getDeadline().isAfter(Instant.now()));

        // Not yet expired: nothing is picked.
        assertFalse(draft.autoPickExpired());

        // Expire the turn: the league picks for the player.
        turn.setDeadline(Instant.now().minusSeconds(1));
        picks.save(turn);
        assertTrue(draft.autoPickExpired());

        DraftPick made = picks.findAllByOrderByOverallAsc().stream()
                .filter(p -> p.getOverall() == turn.getOverall()).findFirst().orElseThrow();
        assertNotNull(made.getTicker(), "the expired pick now has a stock");
        assertEquals(turn.getSpotId(), made.getSpotId());
        assertEquals(LeagueStatus.DRAFTING, league.league().getStatus());

        // The next turn is the player's again (snake order with one human), and it has its own timer.
        DraftPick next = picks.findFirstByStockIdIsNullOrderByOverallAsc().orElseThrow();
        assertNotNull(next.getDeadline());
        assertTrue(next.getDeadline().isAfter(Instant.now()));
    }
}
