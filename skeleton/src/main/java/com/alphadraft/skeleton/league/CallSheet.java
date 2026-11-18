package com.alphadraft.skeleton.league;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// A team's calls for one month: whether they are locked, and how. The picks live in CallPick.
@Entity
@Table(name = "league_call_sheets", uniqueConstraints = @UniqueConstraint(columnNames = {"teamId", "monthIndex"}))
public class CallSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private long teamId;

    @Column(nullable = false)
    private int monthIndex;

    @Column(nullable = false)
    private boolean submitted;

    // "Submitted <time>", "Bot" or "Carried over" for audit and display.
    private String note;

    protected CallSheet() {
    }

    public CallSheet(long teamId, int monthIndex, boolean submitted, String note) {
        this.teamId = teamId;
        this.monthIndex = monthIndex;
        this.submitted = submitted;
        this.note = note;
    }

    public void lock(String note) {
        this.submitted = true;
        this.note = note;
    }

    public void unlock() {
        this.submitted = false;
        this.note = null;
    }

    public long getId() { return id; }
    public long getTeamId() { return teamId; }
    public int getMonthIndex() { return monthIndex; }
    public boolean isSubmitted() { return submitted; }
    public String getNote() { return note; }
}
