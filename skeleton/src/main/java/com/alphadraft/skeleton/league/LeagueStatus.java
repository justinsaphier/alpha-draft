package com.alphadraft.skeleton.league;

// REGISTRATION: players request to join and the admin accepts them.
// DRAFTING: the snake draft is running (bots pick automatically on their turn).
// PRESEASON: the draft is done; calls for the first month can be set.
// IN_SEASON: a month is live (currentMonth 0..2).
// COMPLETE: all three months are scored.
public enum LeagueStatus { REGISTRATION, DRAFTING, PRESEASON, IN_SEASON, COMPLETE }
