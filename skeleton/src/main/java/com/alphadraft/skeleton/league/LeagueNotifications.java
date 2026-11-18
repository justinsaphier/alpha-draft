package com.alphadraft.skeleton.league;

import java.util.List;
import org.springframework.stereotype.Component;

// The league's messages to players: the draft starting, your pick, an automatic pick, and a scored month.
// Only accepted human players are emailed; bots have no address.
@Component
public class LeagueNotifications {

    private final Notifier notifier;

    public LeagueNotifications(Notifier notifier) {
        this.notifier = notifier;
    }

    public void draftStarted(List<Team> teams) {
        for (Team team : teams) {
            toPlayer(team, "The Alpha Draft league draft has started",
                    "Hi " + team.getName() + ",\n\nThe snake draft has started. When it is your turn, you have "
                            + "the pick timer to choose a stock. Sign in to the Draft room to pick.");
        }
    }

    public void yourPick(Team team, String spotLabel, int pickSeconds) {
        toPlayer(team, "Your draft pick",
                "It is your pick for " + spotLabel + ". You have " + pickSeconds + " seconds. "
                        + "Sign in to the Draft room to choose a stock.");
    }

    public void autoPicked(Team team, String ticker) {
        toPlayer(team, "Your draft pick was made for you",
                "Your pick ran out of time, so " + ticker + " was drafted for " + team.getName() + ".");
    }

    public void monthScored(Team team, String monthLabel, double points, String nextLabel) {
        String next = nextLabel == null
                ? "That was the last month of the season."
                : "Calls for " + nextLabel + " are open. Submit them before the month starts.";
        toPlayer(team, monthLabel + " is scored",
                "Your team, " + team.getName() + ", scored " + String.format("%+.1f", points) + " points in "
                        + monthLabel + ".\n\n" + next);
    }

    private void toPlayer(Team team, String subject, String body) {
        if (!team.isBot() && team.getOwnerEmail() != null) {
            notifier.send(team.getOwnerEmail(), subject, body);
        }
    }
}
