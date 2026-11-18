package com.alphadraft.skeleton.league;

import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.FetchType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// One roster stock in a month's calls: long or short, and whether it is the month's 2x pick.
@Entity
@Table(name = "league_call_picks", uniqueConstraints = @UniqueConstraint(columnNames = {"teamId", "monthIndex", "ticker"}))
public class CallPick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private long teamId;

    // The team this row belongs to. The column above stores the id; this association lets JPA load the team.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teamId", insertable = false, updatable = false)
    private Team team;

    @Column(nullable = false)
    private int monthIndex;

    @Column(nullable = false)
    private String ticker;

    // "LONG" or "SHORT".
    @Column(nullable = false)
    private String direction;

    @Column(nullable = false)
    private boolean levered;

    protected CallPick() {
    }

    public CallPick(long teamId, int monthIndex, String ticker, String direction, boolean levered) {
        this.teamId = teamId;
        this.monthIndex = monthIndex;
        this.ticker = ticker;
        this.direction = direction;
        this.levered = levered;
    }

    public void set(String direction, boolean levered) {
        this.direction = direction;
        this.levered = levered;
    }

    public Team getTeam() { return team; }
    public long getId() { return id; }
    public long getTeamId() { return teamId; }
    public int getMonthIndex() { return monthIndex; }
    public String getTicker() { return ticker; }
    public String getDirection() { return direction; }
    public boolean isLevered() { return levered; }
}
