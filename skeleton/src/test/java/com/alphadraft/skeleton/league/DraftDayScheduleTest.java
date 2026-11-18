package com.alphadraft.skeleton.league;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// The admin sets a draft time; the scheduler starts the draft when it arrives.
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:draft-schedule-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes-long",
    "app.jwt.expiration-minutes=60",
    "app.league.draft-check-ms=500"
})
class DraftDayScheduleTest {

    @Autowired LeagueService league;
    @Autowired TeamRepository teams;

    @Test
    void aDraftTimeInThePastIsRefused() {
        assertThrows(LeagueException.class, () ->
                league.updateSettings("Late League", 8, Instant.now().minusSeconds(60), "2026-12"));
    }

    @Test
    void theDraftStartsByItselfAtTheSetTime() throws InterruptedException {
        league.join("sched-player@league.test", "Scheduled Team");
        league.accept(teams.findByOwnerEmailIgnoreCase("sched-player@league.test").orElseThrow().getId());
        league.updateSettings("Scheduled League", 6, Instant.now().plusSeconds(3), "2026-12");

        assertEquals(LeagueStatus.REGISTRATION, league.league().getStatus());
        long deadline = System.currentTimeMillis() + 20_000;
        while (league.league().getStatus() == LeagueStatus.REGISTRATION && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        assertEquals(LeagueStatus.DRAFTING, league.league().getStatus());
        List<Team> seated = league.seatedTeams();
        assertEquals(6, seated.size());
    }
}
