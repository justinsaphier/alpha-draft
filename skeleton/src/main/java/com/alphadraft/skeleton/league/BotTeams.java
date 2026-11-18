package com.alphadraft.skeleton.league;

import java.util.List;

// Names and colours for the computer teams that fill a league, and for the human teams, which
// take the next colour in the list. A league can hold up to ten teams.
public final class BotTeams {

    public record Identity(String name, String color) {
    }

    public static final List<Identity> BOTS = List.of(
            new Identity("Short Squeeze Syndicate", "#10b981"),
            new Identity("Bull Market Bandits", "#60a5fa"),
            new Identity("Dividend Dynasty", "#f472b6"),
            new Identity("Leveraged Legends", "#fbbf24"),
            new Identity("Alpha Hunters", "#a78bfa"),
            new Identity("Bear Necessities", "#f87171"),
            new Identity("Green Day Traders", "#22d3ee"),
            new Identity("Diamond Hands FC", "#fb923c"),
            new Identity("Index Huggers", "#34d399"),
            new Identity("Value Vultures", "#94a3b8"));

    public static final List<String> PLAYER_COLORS = List.of(
            "#0ea5e9", "#84cc16", "#e879f9", "#f97316", "#14b8a6",
            "#eab308", "#ef4444", "#6366f1", "#ec4899", "#22c55e");

    private BotTeams() {
    }
}
