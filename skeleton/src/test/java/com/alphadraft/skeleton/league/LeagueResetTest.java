package com.alphadraft.skeleton.league;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Deleting the league clears everything it built and reopens a fresh one, but only when the admin
// types the league's exact name.
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:league-reset-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes-long",
    "app.jwt.expiration-minutes=60",
    "app.league.draft-check-ms=3600000",
    "app.league.pick-check-ms=3600000"
})
class LeagueResetTest {

    @Autowired LeagueService league;
    @Autowired LeagueResetService reset;
    @Autowired TeamRepository teams;
    @Autowired DraftPickRepository picks;

    @Test
    void theLeagueNameMustBeTypedToDeleteIt() {
        league.updateSettings("Keep Me", 6, null, "2026-12", 60);
        assertThrows(LeagueException.class, () -> reset.reset("keep me"));
        assertThrows(LeagueException.class, () -> reset.reset(null));
        assertEquals("Keep Me", league.league().getName());
    }

    @Test
    void deletingTheLeagueClearsItAndReopensAFreshOne() {
        league.join("reset-player@league.test", "Doomed Team");
        league.accept(teams.findByOwnerEmailIgnoreCase("reset-player@league.test").orElseThrow().getId());
        league.updateSettings("Doomed League", 6, null, "2026-12", 60);
        assertTrue(teams.count() > 0);

        reset.reset("Doomed League");

        assertEquals(0, teams.count());
        assertEquals(0, picks.count());
        assertEquals(LeagueStatus.REGISTRATION, league.league().getStatus());
        assertEquals(League.DEFAULT_NAME, league.league().getName());
        assertEquals(League.DEFAULT_SIZE, league.league().getLeagueSize());
        assertEquals(-1, league.league().getCurrentMonth());
        assertEquals(1, league.recentAudit().size(), "only the reset itself remains in the log");
    }
}
