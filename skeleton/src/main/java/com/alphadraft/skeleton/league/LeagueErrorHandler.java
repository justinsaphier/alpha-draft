package com.alphadraft.skeleton.league;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Turns league rule violations into a { "message": ... } body with the right status. The frontend
// shows the message to the user as is.
@RestControllerAdvice(assignableTypes = {LeagueController.class, AdminLeagueController.class})
public class LeagueErrorHandler {

    @ExceptionHandler(LeagueException.class)
    public ResponseEntity<Map<String, String>> handle(LeagueException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }
}
