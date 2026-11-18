package com.alphadraft.skeleton.league;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Starts the draft by itself once the admin's draft time has passed.
@Component
public class LeagueScheduler {

    private static final Logger log = LoggerFactory.getLogger(LeagueScheduler.class);

    private final LeagueService league;
    private final DraftService draft;
    private final Clock clock;

    public LeagueScheduler(LeagueService league, DraftService draft, Clock clock) {
        this.league = league;
        this.draft = draft;
        this.clock = clock;
    }

    // The first check waits a minute so the league row exists before it is read.
    @Scheduled(initialDelayString = "${app.league.draft-check-ms:60000}", fixedDelayString = "${app.league.draft-check-ms:60000}")
    public void startDraftWhenDue() {
        League current;
        try {
            current = league.league();
        } catch (IllegalStateException e) {
            log.warn("Draft check skipped: {}", e.getMessage());
            return;
        }
        if (current.getStatus() != LeagueStatus.REGISTRATION || current.getDraftAt() == null) {
            return;
        }
        if (clock.instant().isBefore(current.getDraftAt())) {
            return;
        }
        try {
            draft.startDraft();
            log.info("Draft started automatically at the scheduled time");
        } catch (LeagueException e) {
            log.warn("Scheduled draft not started: {}", e.getMessage());
        }
    }
}
