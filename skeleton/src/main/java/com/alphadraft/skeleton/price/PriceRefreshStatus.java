package com.alphadraft.skeleton.price;

import java.time.Instant;

// What the price page shows about the latest closing-price update.
public record PriceRefreshStatus(
        boolean configured,
        boolean running,
        Instant lastStartedAt,
        Instant lastFinishedAt,
        Integer lastStored,
        String lastError) {
}
