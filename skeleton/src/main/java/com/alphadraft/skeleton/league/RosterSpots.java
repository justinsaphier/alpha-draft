package com.alphadraft.skeleton.league;

import java.util.List;

// The six roster spots. Every team drafts one stock from each spot, in round order, so a roster
// covers all 11 GICS sectors in six groups.
public final class RosterSpots {

    public record Spot(int id, String label, List<String> sectors) {
    }

    public static final List<Spot> ALL = List.of(
            new Spot(1, "Technology", List.of("Technology")),
            new Spot(2, "Financials / Real Estate", List.of("Financials", "Real Estate")),
            new Spot(3, "Healthcare / Consumer Staples", List.of("Healthcare", "Consumer Staples")),
            new Spot(4, "Energy / Materials", List.of("Energy", "Materials")),
            new Spot(5, "Industrials / Consumer Discretionary", List.of("Industrials", "Consumer Discretionary")),
            new Spot(6, "Utilities / Communication Services", List.of("Utilities", "Communication Services")));

    private RosterSpots() {
    }

    // The spot id for a sector, or 0 when the sector belongs to no spot.
    public static int spotForSector(String sector) {
        return ALL.stream().filter(spot -> spot.sectors().contains(sector)).findFirst().map(Spot::id).orElse(0);
    }

    public static String labelFor(int spotId) {
        return ALL.stream().filter(spot -> spot.id() == spotId).findFirst().map(Spot::label).orElse("");
    }
}
