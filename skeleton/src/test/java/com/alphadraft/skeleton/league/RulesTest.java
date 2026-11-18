package com.alphadraft.skeleton.league;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RulesTest {

    @Test
    void aLongCallGainsWithThePriceAndAShortLosesIt() {
        assertEquals(10.0, Scoring.points(0.10, "LONG", false), 1e-9);
        assertEquals(-10.0, Scoring.points(0.10, "SHORT", false), 1e-9);
    }

    @Test
    void theLeveredCallCountsDouble() {
        assertEquals(20.0, Scoring.points(0.10, "LONG", true), 1e-9);
        assertEquals(20.0, Scoring.points(-0.10, "SHORT", true), 1e-9);
    }

    @Test
    void snakeOrderReversesEveryOtherRound() {
        List<SnakeDraft.Slot> slots = SnakeDraft.order(List.of(1L, 2L, 3L), 2);

        assertEquals(List.of(1L, 2L, 3L, 3L, 2L, 1L), slots.stream().map(SnakeDraft.Slot::teamId).toList());
        assertEquals(List.of(1, 2, 3, 4, 5, 6), slots.stream().map(SnakeDraft.Slot::overall).toList());
        assertEquals(1, slots.get(0).spotId());
        assertEquals(2, slots.get(3).spotId());
    }

    @Test
    void everySectorBelongsToExactlyOneRosterSpot() {
        List<String> sectors = List.of("Technology", "Financials", "Real Estate", "Healthcare", "Consumer Staples",
                "Energy", "Materials", "Industrials", "Consumer Discretionary", "Utilities", "Communication Services");

        for (String sector : sectors) {
            assertEquals(false, RosterSpots.spotForSector(sector) == 0, sector);
        }
        assertEquals(0, RosterSpots.spotForSector("Crypto"));
        assertEquals(6, RosterSpots.ALL.size());
    }
}
