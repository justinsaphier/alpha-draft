package com.alphadraft.skeleton.controller;

import com.alphadraft.skeleton.model.DailyPrice;
import com.alphadraft.skeleton.price.PriceRefreshService;
import com.alphadraft.skeleton.price.PriceRefreshService.StartOutcome;
import com.alphadraft.skeleton.price.PriceRefreshStatus;
import com.alphadraft.skeleton.repository.DailyPriceRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Closing prices: any signed-in user can read them; only an ADMIN can start a refresh.
// Access rules are in SecurityConfig.
@RestController
@RequestMapping("/prices")
public class PriceController {

    private final DailyPriceRepository prices;
    private final PriceRefreshService refresh;

    public PriceController(DailyPriceRepository prices, PriceRefreshService refresh) {
        this.prices = prices;
        this.refresh = refresh;
    }

    // The most recent closing price for every stock that has one.
    @GetMapping("/latest")
    public List<DailyPrice> latest() {
        return prices.findLatestForEachStock();
    }

    @GetMapping("/refresh")
    public PriceRefreshStatus status() {
        return refresh.status();
    }

    // 202 when the refresh starts, 409 when one is already running, 503 when no API key is set.
    @PostMapping("/refresh")
    public ResponseEntity<PriceRefreshStatus> startRefresh() {
        StartOutcome outcome = refresh.start();
        HttpStatus status = switch (outcome) {
            case STARTED -> HttpStatus.ACCEPTED;
            case ALREADY_RUNNING -> HttpStatus.CONFLICT;
            case NOT_CONFIGURED -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return ResponseEntity.status(status).body(refresh.status());
    }
}
