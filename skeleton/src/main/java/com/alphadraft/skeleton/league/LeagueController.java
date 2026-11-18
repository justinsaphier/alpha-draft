package com.alphadraft.skeleton.league;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.DailyPriceRepository;
import com.alphadraft.skeleton.repository.StockRepository;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The player's view of the league: joining, the draft, monthly calls, and standings.
// Admin decisions live in AdminLeagueController.
@RestController
@RequestMapping("/league")
public class LeagueController {

    public record TeamRow(long id, String name, String color, boolean bot, String status) {
    }

    public record RosterSpot(int spot, String label, String ticker, String name, java.math.BigDecimal price) {
    }

    public record MyTeam(long id, String name, String color, List<RosterSpot> roster) {
    }

    public record Summary(String name, LeagueStatus status, int leagueSize, Instant draftAt, String firstMonth,
                          int currentMonth, List<String> monthLabels, int finalizedMonths, int acceptedCount,
                          MyTeam myTeam, String myRequest, List<TeamRow> teams) {
    }

    public record DraftPickView(int overall, int round, int spot, String spotLabel, long teamId, String teamName,
                                String teamColor, String ticker) {
    }

    public record Eligible(String ticker, String name, String sector) {
    }

    public record DraftView(LeagueStatus status, Integer currentOverall, boolean myTurn, String onTheClock,
                            Instant onTheClockUntil, List<DraftPickView> picks, List<Eligible> eligible) {
    }

    public record OwnerRow(String ticker, long teamId, String teamName, String teamColor) {
    }

    public record PickBody(String ticker) {
    }

    public record CallsBody(Map<String, String> directions, String levered) {
    }

    private final LeagueService league;
    private final DraftService draft;
    private final CallService calls;
    private final SeasonService season;
    private final StockRepository stocks;
    private final DailyPriceRepository prices;

    public LeagueController(LeagueService league, DraftService draft, CallService calls,
                            SeasonService season, StockRepository stocks, DailyPriceRepository prices) {
        this.league = league;
        this.draft = draft;
        this.calls = calls;
        this.season = season;
        this.stocks = stocks;
        this.prices = prices;
    }

    @GetMapping
    public Summary summary(Principal principal) {
        return buildSummary(principal.getName());
    }

    @PostMapping("/join")
    public ResponseEntity<Summary> join(Principal principal, @RequestBody Map<String, String> body) {
        league.join(principal.getName(), body == null ? null : body.get("teamName"));
        return ResponseEntity.status(HttpStatus.CREATED).body(buildSummary(principal.getName()));
    }

    @GetMapping("/draft")
    public DraftView draftBoard(Principal principal) {
        return buildDraft(principal.getName());
    }

    @PostMapping("/draft/picks")
    public DraftView pick(Principal principal, @RequestBody PickBody body) {
        draft.pick(principal.getName(), body == null ? null : body.ticker());
        return buildDraft(principal.getName());
    }

    @GetMapping("/owners")
    public List<OwnerRow> owners() {
        List<OwnerRow> rows = new ArrayList<>();
        Map<Long, Team> teams = teamIndex();
        for (DraftPick pick : draft.board()) {
            if (pick.getTicker() != null) {
                Team team = teams.get(pick.getTeamId());
                rows.add(new OwnerRow(pick.getTicker(), pick.getTeamId(), team.getName(), team.getColor()));
            }
        }
        return rows;
    }

    // This month's calls priced against today's closes, for the signed-in player's team.
    @GetMapping("/live")
    public List<SeasonService.LiveRow> live(Principal principal) {
        return league.teamOf(principal.getName())
                .filter(t -> t.getStatus() == TeamStatus.ACCEPTED)
                .map(t -> season.liveBreakdown(t.getId()))
                .orElse(List.of());
    }

    @GetMapping("/standings")
    public List<SeasonService.StandingRow> standings() {
        return season.standings();
    }

    @GetMapping("/calls/{month}")
    public CallService.CallSheetView myCalls(Principal principal, @PathVariable int month) {
        return calls.viewOwn(principal.getName(), month);
    }

    @PutMapping("/calls/{month}")
    public CallService.CallSheetView saveCalls(Principal principal, @PathVariable int month,
                                               @RequestBody CallsBody body) {
        return calls.save(principal.getName(), month,
                body == null ? null : body.directions(), body == null ? null : body.levered());
    }

    @PostMapping("/calls/{month}/submit")
    public CallService.CallSheetView submitCalls(Principal principal, @PathVariable int month) {
        return calls.submit(principal.getName(), month);
    }

    @GetMapping("/teams/{teamId}/calls/{month}")
    public CallService.CallSheetView teamCalls(Principal principal, @PathVariable long teamId, @PathVariable int month) {
        return calls.viewFor(principal.getName(), teamId, month);
    }

    private Summary buildSummary(String email) {
        League current = league.league();
        List<String> labels = new ArrayList<>();
        for (int m = 0; m < League.MONTHS_IN_SEASON; m++) {
            labels.add(calls.label(m));
        }
        Team mine = league.teamOf(email).orElse(null);
        MyTeam myTeam = null;
        String myRequest = null;
        if (mine != null) {
            if (mine.getStatus() == TeamStatus.ACCEPTED) {
                myTeam = new MyTeam(mine.getId(), mine.getName(), mine.getColor(), rosterSpots(mine.getId()));
            } else {
                myRequest = mine.getStatus().name();
            }
        }
        List<TeamRow> teams = new ArrayList<>();
        for (Team team : league.seatedTeams()) {
            teams.add(new TeamRow(team.getId(), team.getName(), team.getColor(), team.isBot(), team.getStatus().name()));
        }
        int accepted = (int) league.seatedTeams().stream().filter(t -> !t.isBot()).count();
        return new Summary(current.getName(), current.getStatus(), current.getLeagueSize(), current.getDraftAt(),
                current.getFirstMonth(), current.getCurrentMonth(), labels, season.finalizedMonths(current), accepted,
                myTeam, myRequest, teams);
    }

    private List<RosterSpot> rosterSpots(long teamId) {
        Map<Integer, Stock> byId = stockIndex();
        Map<Integer, java.math.BigDecimal> latest = new java.util.HashMap<>();
        for (com.alphadraft.skeleton.model.DailyPrice price : prices.findLatestForEachStock()) {
            latest.put(price.getStockId(), price.getClosePrice());
        }
        List<RosterSpot> out = new ArrayList<>();
        for (DraftPick pick : calls.rosterPicks(teamId)) {
            Stock stock = byId.get(pick.getStockId());
            out.add(new RosterSpot(pick.getSpotId(), RosterSpots.labelFor(pick.getSpotId()), pick.getTicker(),
                    stock == null ? "" : stock.getName(), latest.get(pick.getStockId())));
        }
        return out;
    }

    private DraftView buildDraft(String email) {
        League current = league.league();
        Map<Long, Team> teams = teamIndex();
        Map<Integer, Stock> byId = stockIndex();
        List<DraftPick> board = draft.board();
        List<DraftPickView> picks = new ArrayList<>();
        DraftPick turn = null;
        Set<Integer> taken = new HashSet<>();
        for (DraftPick pick : board) {
            Team team = teams.get(pick.getTeamId());
            picks.add(new DraftPickView(pick.getOverall(), pick.getRound(), pick.getSpotId(),
                    RosterSpots.labelFor(pick.getSpotId()), pick.getTeamId(), team.getName(), team.getColor(), pick.getTicker()));
            if (pick.getStockId() != null) {
                taken.add(pick.getStockId());
            } else if (turn == null) {
                turn = pick;
            }
        }
        boolean myTurn = false;
        String onClock = null;
        List<Eligible> eligible = new ArrayList<>();
        if (turn != null && current.getStatus() == LeagueStatus.DRAFTING) {
            Team onTurn = teams.get(turn.getTeamId());
            onClock = onTurn.getName();
            myTurn = onTurn.getOwnerEmail() != null && onTurn.getOwnerEmail().equalsIgnoreCase(email);
            if (myTurn) {
                for (Stock stock : byId.values()) {
                    if (RosterSpots.spotForSector(stock.getSector()) == turn.getSpotId() && !taken.contains(stock.getId())) {
                        eligible.add(new Eligible(stock.getTicker(), stock.getName(), stock.getSector()));
                    }
                }
                eligible.sort(java.util.Comparator.comparing(Eligible::ticker));
            }
        }
        Instant until = turn == null ? null : turn.getDeadline();
        return new DraftView(current.getStatus(), turn == null ? null : turn.getOverall(), myTurn, onClock, until, picks, eligible);
    }

    private Map<Long, Team> teamIndex() {
        Map<Long, Team> out = new java.util.HashMap<>();
        for (Team team : league.seatedTeams()) {
            out.put(team.getId(), team);
        }
        return out;
    }

    private Map<Integer, Stock> stockIndex() {
        Map<Integer, Stock> out = new java.util.HashMap<>();
        for (Stock stock : stocks.findAll()) {
            out.put(stock.getId(), stock);
        }
        return out;
    }
}
