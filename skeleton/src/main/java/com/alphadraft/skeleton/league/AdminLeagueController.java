package com.alphadraft.skeleton.league;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.StockRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The admin console: accept or turn down players, set the league and the draft day, start the draft
// and the season, close months, correct prices, and read the league's audit log. ADMIN only.
@RestController
@RequestMapping("/admin/league")
public class AdminLeagueController {

    public record PlayerRow(long id, String name, String email, String status) {
    }

    public record AuditRow(Instant at, String action, String detail) {
    }

    public record AdminView(String name, LeagueStatus status, int leagueSize, Instant draftAt, String firstMonth,
                            int pickSeconds, int currentMonth, List<String> monthLabels, List<PlayerRow> pending, List<PlayerRow> accepted,
                            int botCount, List<String> missingPrices, List<SeasonService.PriceRow> prices,
                            List<AuditRow> audit) {
    }

    public record SettingsBody(String name, int leagueSize, Instant draftAt, String firstMonth, Integer pickSeconds) {
    }

    public record CloseBody(BigDecimal close) {
    }

    private final LeagueService league;
    private final DraftService draft;
    private final SeasonService season;
    private final CallService calls;
    private final StockRepository stocks;

    public AdminLeagueController(LeagueService league, DraftService draft, SeasonService season,
                                 CallService calls, StockRepository stocks) {
        this.league = league;
        this.draft = draft;
        this.season = season;
        this.calls = calls;
        this.stocks = stocks;
    }

    @GetMapping
    public AdminView view() {
        League current = league.league();
        List<String> labels = new ArrayList<>();
        for (int m = 0; m < League.MONTHS_IN_SEASON; m++) {
            labels.add(calls.label(m));
        }
        List<PlayerRow> pending = league.teamsWithStatus(TeamStatus.PENDING).stream()
                .map(t -> new PlayerRow(t.getId(), t.getName(), t.getOwnerEmail(), t.getStatus().name())).toList();
        List<PlayerRow> accepted = league.teamsWithStatus(TeamStatus.ACCEPTED).stream()
                .map(t -> new PlayerRow(t.getId(), t.getName(), t.getOwnerEmail(), t.getStatus().name())).toList();
        int bots = (int) league.seatedTeams().stream().filter(Team::isBot).count();
        List<AuditRow> audit = league.recentAudit().stream()
                .map(a -> new AuditRow(a.getAt(), a.getAction(), a.getDetail())).toList();
        return new AdminView(current.getName(), current.getStatus(), current.getLeagueSize(), current.getDraftAt(),
                current.getFirstMonth(), current.getPickSeconds(), current.getCurrentMonth(), labels, pending, accepted, bots,
                season.missingPrices(), season.priceTable(), audit);
    }

    @PutMapping("/settings")
    public ResponseEntity<Void> settings(@RequestBody SettingsBody body) {
        int seconds = body.pickSeconds() == null ? 90 : body.pickSeconds();
        league.updateSettings(body.name(), body.leagueSize(), body.draftAt(), body.firstMonth(), seconds);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/teams/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable long id) {
        league.accept(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/teams/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable long id) {
        league.reject(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/draft/start")
    public ResponseEntity<Void> startDraft() {
        draft.startDraft();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/season/start")
    public ResponseEntity<Void> startSeason() {
        season.startSeason();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/months/finalize")
    public ResponseEntity<Void> finalizeMonth() {
        season.finalizeMonth();
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/snapshots/{ticker}/{month}")
    public ResponseEntity<Void> setPrice(@PathVariable String ticker, @PathVariable int month, @RequestBody CloseBody body) {
        season.overridePrice(ticker, month, body == null ? null : body.close());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/snapshots/{ticker}/{month}")
    public ResponseEntity<Void> clearPrice(@PathVariable String ticker, @PathVariable int month) {
        season.clearOverride(ticker, month);
        return ResponseEntity.noContent().build();
    }
}
