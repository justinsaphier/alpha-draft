package com.alphadraft.skeleton.league;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

// One slot in the snake draft. stockId and ticker stay null until the pick is made.
@Entity
@Table(name = "league_draft_picks")
public class DraftPick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private int overall;

    @Column(nullable = false)
    private int round;

    @Column(nullable = false)
    private long teamId;

    @Column(nullable = false)
    private int spotId;

    private Integer stockId;

    private String ticker;

    private Instant madeAt;

    // When a player's turn runs out. Set when the pick comes on the clock; null for other picks.
    private Instant deadline;

    protected DraftPick() {
    }

    public DraftPick(int overall, int round, long teamId, int spotId) {
        this.overall = overall;
        this.round = round;
        this.teamId = teamId;
        this.spotId = spotId;
    }

    public void make(int stockId, String ticker, Instant at) {
        this.stockId = stockId;
        this.ticker = ticker;
        this.madeAt = at;
    }

    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }
    public long getId() { return id; }
    public int getOverall() { return overall; }
    public int getRound() { return round; }
    public long getTeamId() { return teamId; }
    public int getSpotId() { return spotId; }
    public Integer getStockId() { return stockId; }
    public String getTicker() { return ticker; }
}
