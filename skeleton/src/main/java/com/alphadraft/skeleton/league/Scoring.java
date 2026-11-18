package com.alphadraft.skeleton.league;

// points = (±return in %) x leverage. A short call flips the sign; the month's 2x pick counts double.
public final class Scoring {

    private Scoring() {
    }

    public static double points(double monthReturn, String direction, boolean levered) {
        double sign = "SHORT".equals(direction) ? -1 : 1;
        return sign * monthReturn * 100 * (levered ? 2 : 1);
    }
}
