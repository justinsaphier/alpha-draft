package com.alphadraft.skeleton.league;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

// A record of what happened in the league: joins, the draft, lock-ins, scoring and price overrides.
@Entity
@Table(name = "league_audit")
public class LeagueAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private Instant at;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false, length = 1000)
    private String detail;

    protected LeagueAudit() {
    }

    public LeagueAudit(Instant at, String action, String detail) {
        this.at = at;
        this.action = action;
        this.detail = detail;
    }

    public long getId() { return id; }
    public Instant getAt() { return at; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
}
