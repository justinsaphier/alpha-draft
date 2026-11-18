package com.alphadraft.skeleton.league;

import org.springframework.http.HttpStatus;

// A rule the league enforces (for example "not your pick" or "calls are locked"). The message is
// shown to the user as is.
public class LeagueException extends RuntimeException {

    private final HttpStatus status;

    public LeagueException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
