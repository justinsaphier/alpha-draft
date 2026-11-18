package com.alphadraft.skeleton.league;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Creates the league the first time the app starts. Later starts leave it alone.
@Component
public class LeagueSeeder implements CommandLineRunner {

    private final LeagueRepository leagues;

    public LeagueSeeder(LeagueRepository leagues) {
        this.leagues = leagues;
    }

    @Override
    public void run(String... args) {
        if (!leagues.existsById(League.ID)) {
            leagues.save(new League("Alpha Draft League", 8, LeagueStatus.REGISTRATION));
        }
    }
}
