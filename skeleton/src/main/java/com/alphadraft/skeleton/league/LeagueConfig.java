package com.alphadraft.skeleton.league;

import java.time.Clock;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Shared collaborators for the league: a clock (so tests can fix the time) and the randomness used
// for the draft order and the bots' picks.
@Configuration
public class LeagueConfig {

    @Bean
    public Clock leagueClock() {
        return Clock.systemUTC();
    }

    @Bean
    public Random leagueRandom() {
        return new Random();
    }
}
