package com.alphadraft.skeleton.league;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

// The single league (row id 1). Settings the admin controls plus the season's progress.
@Entity
@Table(name = "leagues")
public class League {

    public static final int ID = 1;
    public static final int MONTHS_IN_SEASON = 3;

    @Id
    private int id = ID;

    private String name;

    private int leagueSize;

    @Enumerated(EnumType.STRING)
    private LeagueStatus status;

    // When the draft starts on its own. Null means the admin starts it by hand.
    private Instant draftAt;

    // "yyyy-MM" of the first month of the season, e.g. "2026-12".
    private String firstMonth;

    // -1 before the season, otherwise the live month (0..2).
    private int currentMonth = -1;

    // Seconds a player has to make each draft pick. Null on rows created before the timer existed.
    private Integer pickSeconds;

    protected League() {
    }

    public League(String name, int leagueSize, LeagueStatus status) {
        this.name = name;
        this.leagueSize = leagueSize;
        this.status = status;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLeagueSize() { return leagueSize; }
    public void setLeagueSize(int leagueSize) { this.leagueSize = leagueSize; }
    public LeagueStatus getStatus() { return status; }
    public void setStatus(LeagueStatus status) { this.status = status; }
    public Instant getDraftAt() { return draftAt; }
    public void setDraftAt(Instant draftAt) { this.draftAt = draftAt; }
    public String getFirstMonth() { return firstMonth; }
    public void setFirstMonth(String firstMonth) { this.firstMonth = firstMonth; }
    // Back to a fresh league: the default name and size, open for sign-ups, no draft, no season.
    public void resetToDefaults() {
        this.name = DEFAULT_NAME;
        this.leagueSize = DEFAULT_SIZE;
        this.status = LeagueStatus.REGISTRATION;
        this.draftAt = null;
        this.firstMonth = null;
        this.currentMonth = -1;
        this.pickSeconds = null;
    }

    public static final String DEFAULT_NAME = "Alpha Draft League";
    public static final int DEFAULT_SIZE = 8;

    public int getPickSeconds() { return pickSeconds == null ? 90 : pickSeconds; }
    public void setPickSeconds(int pickSeconds) { this.pickSeconds = pickSeconds; }
    public int getCurrentMonth() { return currentMonth; }
    public void setCurrentMonth(int currentMonth) { this.currentMonth = currentMonth; }
}
