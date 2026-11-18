package com.alphadraft.skeleton.league;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Deletes everything the league has built: teams, draft, calls, prices and history. The stock pool
// is kept. Afterwards the league is a fresh one, open for sign-ups. This cannot be undone.
@Service
public class LeagueResetService {

    private final LeagueService league;
    private final CallPickRepository callPicks;
    private final CallSheetRepository callSheets;
    private final PriceSnapshotRepository snapshots;
    private final DraftPickRepository draftPicks;
    private final TeamRepository teams;
    private final LeagueAuditRepository audit;

    public LeagueResetService(LeagueService league, CallPickRepository callPicks, CallSheetRepository callSheets,
                              PriceSnapshotRepository snapshots, DraftPickRepository draftPicks,
                              TeamRepository teams, LeagueAuditRepository audit) {
        this.league = league;
        this.callPicks = callPicks;
        this.callSheets = callSheets;
        this.snapshots = snapshots;
        this.draftPicks = draftPicks;
        this.teams = teams;
        this.audit = audit;
    }

    // The admin must type the league's current name exactly, so a stray click cannot wipe it.
    @Transactional
    public void reset(String confirmName) {
        League current = league.league();
        if (confirmName == null || !confirmName.trim().equals(current.getName())) {
            throw new LeagueException(HttpStatus.BAD_REQUEST,
                    "To delete the league, type its name exactly: " + current.getName());
        }
        callPicks.deleteAll();
        callSheets.deleteAll();
        snapshots.deleteAll();
        draftPicks.deleteAll();
        teams.deleteAll();
        audit.deleteAll();
        current.resetToDefaults();
        league.log("RESET", "League deleted. A new league is open for sign-ups.");
    }
}
