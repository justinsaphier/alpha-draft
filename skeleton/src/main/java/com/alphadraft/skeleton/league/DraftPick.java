package com.alphadraft.skeleton.league;

import jakarta.persistence.Column;
import com.alphadraft.skeleton.model.Stock;
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

    // The team this row belongs to. The column above stores the id; this association lets JPA load the team.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teamId", insertable = false, updatable = false)
    private Team team;

    @Column(nullable = false)
    private int spotId;

    private Integer stockId;

    // The stock this row refers to. Read-only; the id above is what is stored. No database foreign key,
    // so deleting a stock from the pool keeps the price history.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stockId", insertable = false, updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Stock stock;

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
    public Team getTeam() { return team; }
    public Stock getStock() { return stock; }
    public Integer getStockId() { return stockId; }
    public String getTicker() { return ticker; }
}
