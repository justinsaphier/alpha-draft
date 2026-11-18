package com.alphadraft.skeleton.league;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

// A team in the league: a player's team (ownerEmail set) or a bot team (bot = true).
@Entity
@Table(name = "league_teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String color;

    // The signed-in player who owns the team. Null for bots. Never sent to other players.
    @Column(unique = true)
    private String ownerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamStatus status;

    @Column(nullable = false)
    private boolean bot;

    @Column(nullable = false)
    private Instant requestedAt;

    protected Team() {
    }

    public Team(String name, String color, String ownerEmail, TeamStatus status, boolean bot, Instant requestedAt) {
        this.name = name;
        this.color = color;
        this.ownerEmail = ownerEmail;
        this.status = status;
        this.bot = bot;
        this.requestedAt = requestedAt;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public String getOwnerEmail() { return ownerEmail; }
    public TeamStatus getStatus() { return status; }
    public void setStatus(TeamStatus status) { this.status = status; }
    public boolean isBot() { return bot; }
    public Instant getRequestedAt() { return requestedAt; }
}
