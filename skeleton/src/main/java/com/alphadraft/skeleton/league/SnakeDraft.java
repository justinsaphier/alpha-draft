package com.alphadraft.skeleton.league;

import java.util.ArrayList;
import java.util.List;

// Snake order: round 1 runs first to last, round 2 last to first, and so on.
public final class SnakeDraft {

    public record Slot(int overall, int round, long teamId, int spotId) {
    }

    private SnakeDraft() {
    }

    public static List<Slot> order(List<Long> teamIds, int rounds) {
        List<Slot> slots = new ArrayList<>();
        int overall = 1;
        for (int round = 0; round < rounds; round++) {
            List<Long> sequence = new ArrayList<>(teamIds);
            if (round % 2 == 1) {
                java.util.Collections.reverse(sequence);
            }
            int spotId = RosterSpots.ALL.get(round).id();
            for (long teamId : sequence) {
                slots.add(new Slot(overall++, round + 1, teamId, spotId));
            }
        }
        return slots;
    }
}
