package com.alphadraft.skeleton.league;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Background work for the league: starts the draft at the admin's draft time, picks for players
// whose timer has run out, and closes a month once its prices are in.
@Component
public class LeagueScheduler {

    private static final Logger log = LoggerFactory.getLogger(LeagueScheduler.class);
    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");

    private final LeagueService league;
    private final DraftService draft;
    private final SeasonService season;
    private final Clock clock;

    public LeagueScheduler(LeagueService league, DraftService draft, SeasonService season, Clock clock) {
        this.league = league;
        this.draft = draft;
        this.season = season;
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

    @Scheduled(initialDelayString = "${app.league.pick-check-ms:5000}", fixedDelayString = "${app.league.pick-check-ms:5000}")
    public void pickForExpiredTimers() {
        try {
            draft.autoPickExpired();
        } catch (RuntimeException e) {
            log.warn("Timer pick failed: {}", e.getMessage());
        }
    }

    // Every evening in New York: close the live month if it has ended and its prices are current.
    @Scheduled(cron = "${app.league.close-cron:0 0 20 * * *}", zone = "America/New_York")
    public void closeMonthWhenDue() {
        try {
            if (season.finalizeIfDue(LocalDate.now(MARKET_ZONE))) {
                log.info("Month closed automatically");
            }
        } catch (RuntimeException e) {
            log.warn("Automatic month close failed: {}", e.getMessage());
        }
    }
}
