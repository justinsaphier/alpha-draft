package com.alphadraft.skeleton.league;

import com.alphadraft.skeleton.model.DailyPrice;
import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.DailyPriceRepository;
import com.alphadraft.skeleton.repository.StockRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Monthly calls. A team sets long or short on each roster stock and one 2x pick for the month.
// A month's calls are set during the month before it and lock when that month starts. A player who
// never submits is carried over with last month's calls. Bots submit random calls on lock.
@Service
public class CallService {

    public static final String LONG = "LONG";
    public static final String SHORT = "SHORT";

    private final LeagueService league;
    private final CallSheetRepository sheets;
    private final CallPickRepository calls;
    private final DraftPickRepository draftPicks;
    private final StockRepository stocks;
    private final DailyPriceRepository prices;
    private final Clock clock;
    private final Random random;

    public CallService(LeagueService league, CallSheetRepository sheets, CallPickRepository calls,
                       DraftPickRepository draftPicks, StockRepository stocks, DailyPriceRepository prices,
                       Clock clock, Random random) {
        this.league = league;
        this.sheets = sheets;
        this.calls = calls;
        this.draftPicks = draftPicks;
        this.stocks = stocks;
        this.prices = prices;
        this.clock = clock;
        this.random = random;
    }

    // The month whose calls are open right now, or -1 when none are.
    public static int upcomingMonth(League league) {
        return switch (league.getStatus()) {
            case PRESEASON -> 0;
            case IN_SEASON -> league.getCurrentMonth() + 1;
            default -> -1;
        };
    }

    public static boolean isOpen(League league, int month) {
        return month == upcomingMonth(league) && month < League.MONTHS_IN_SEASON;
    }

    @Transactional
    public void lockMonth(int month) {
        for (Team team : league.seatedTeams()) {
            List<String> tickers = rosterTickers(team.getId());
            CallSheet sheet = ensureSheet(team.getId(), month, tickers);
            if (team.isBot()) {
                String levered = tickers.isEmpty() ? null : tickers.get(random.nextInt(tickers.size()));
                applyPicks(team.getId(), month, randomCalls(tickers), levered);
                sheet.lock("Bot calls");
            } else if (!sheet.isSubmitted()) {
                ensureLevered(team.getId(), month, tickers);
                sheet.lock("Locked automatically (calls carried over)");
            }
        }
        league.log("LOCK", label(month) + " calls are locked");
    }

    @Transactional
    public CallSheetView save(String email, int month, Map<String, String> directions, String levered) {
        Team team = memberOrThrow(email);
        if (!isOpen(league.league(), month)) {
            throw new LeagueException(HttpStatus.CONFLICT, "Calls for this month are not open.");
        }
        List<String> tickers = rosterTickers(team.getId());
        CallSheet sheet = ensureSheet(team.getId(), month, tickers);
        if (sheet.isSubmitted()) {
            throw new LeagueException(HttpStatus.CONFLICT, "Your calls for this month are locked.");
        }
        Map<String, String> dirs = new HashMap<>();
        for (String ticker : tickers) {
            String dir = directions == null ? null : directions.get(ticker);
            if (dir == null || !(LONG.equals(dir) || SHORT.equals(dir))) {
                throw new LeagueException(HttpStatus.BAD_REQUEST, "Choose long or short for " + ticker + ".");
            }
            dirs.put(ticker, dir);
        }
        if (levered != null && !tickers.contains(levered)) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, levered + " is not on your roster.");
        }
        applyPicks(team.getId(), month, dirs, levered);
        return view(team, month, true);
    }

    @Transactional
    public CallSheetView submit(String email, int month) {
        Team team = memberOrThrow(email);
        if (!isOpen(league.league(), month)) {
            throw new LeagueException(HttpStatus.CONFLICT, "Calls for this month are not open.");
        }
        CallSheet sheet = ensureSheet(team.getId(), month, rosterTickers(team.getId()));
        if (sheet.isSubmitted()) {
            throw new LeagueException(HttpStatus.CONFLICT, "Your calls for this month are already locked.");
        }
        long levered = calls.findByTeamIdAndMonthIndex(team.getId(), month).stream().filter(CallPick::isLevered).count();
        if (levered != 1) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, "Pick exactly one stock for the 2x call.");
        }
        sheet.lock("Submitted " + clock.instant());
        league.log("CALLS", team.getName() + " submitted " + label(month) + " calls");
        return view(team, month, true);
    }

    // What a player sees for their own team, or what anyone sees for a team after its month has locked.
    public CallSheetView viewFor(String viewerEmail, long teamId, int month) {
        Team team = league.seatedTeams().stream().filter(t -> t.getId() == teamId).findFirst()
                .orElseThrow(() -> new LeagueException(HttpStatus.NOT_FOUND, "No such team."));
        boolean own = viewerEmail != null && viewerEmail.equalsIgnoreCase(team.getOwnerEmail());
        League current = league.league();
        boolean locked = (month >= 0 && month <= current.getCurrentMonth()) || current.getStatus() == LeagueStatus.COMPLETE;
        if (!own && !locked) {
            throw new LeagueException(HttpStatus.FORBIDDEN, "These calls stay hidden until the month locks.");
        }
        if (month < 0 || month >= League.MONTHS_IN_SEASON) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, "There is no such month.");
        }
        return view(team, month, own);
    }

    public CallSheetView viewOwn(String email, int month) {
        Team team = memberOrThrow(email);
        if (month < 0 || month >= League.MONTHS_IN_SEASON) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, "There is no such month.");
        }
        return view(team, month, true);
    }

    public List<String> rosterTickers(long teamId) {
        return rosterPicks(teamId).stream().map(DraftPick::getTicker).toList();
    }

    public List<DraftPick> rosterPicks(long teamId) {
        return draftPicks.findAllByOrderByOverallAsc().stream()
                .filter(p -> p.getTeamId() == teamId && p.getStockId() != null)
                .sorted(Comparator.comparingInt(DraftPick::getSpotId))
                .toList();
    }

    public List<CallPick> picksFor(long teamId, int month) {
        return calls.findByTeamIdAndMonthIndex(teamId, month);
    }

    // Month labels come from the season's first month in the league settings, e.g. "Dec 2026".
    public String label(int month) {
        String first = league.league().getFirstMonth();
        YearMonth start = first == null ? YearMonth.of(2026, 12) : YearMonth.parse(first);
        return start.plusMonths(month).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US));
    }

    private Team memberOrThrow(String email) {
        return league.teamOf(email)
                .filter(t -> t.getStatus() == TeamStatus.ACCEPTED)
                .orElseThrow(() -> new LeagueException(HttpStatus.FORBIDDEN, "Only league members can set calls."));
    }

    // Returns the team's sheet for the month, creating it if needed. A new sheet copies last month's
    // calls, or starts with long on every stock when there is no earlier month.
    private CallSheet ensureSheet(long teamId, int month, List<String> tickers) {
        Optional<CallSheet> existing = sheets.findByTeamIdAndMonthIndex(teamId, month);
        if (existing.isPresent()) {
            return existing.get();
        }
        List<CallPick> previous = calls.findByTeamIdAndMonthIndex(teamId, month - 1);
        Map<String, String> dirs = new HashMap<>();
        String levered = null;
        for (String ticker : tickers) {
            dirs.put(ticker, LONG);
        }
        for (CallPick pick : previous) {
            if (dirs.containsKey(pick.getTicker())) {
                dirs.put(pick.getTicker(), pick.getDirection());
                if (pick.isLevered()) {
                    levered = pick.getTicker();
                }
            }
        }
        applyPicks(teamId, month, dirs, levered);
        return sheets.save(new CallSheet(teamId, month, false, null));
    }

    private void ensureLevered(long teamId, int month, List<String> tickers) {
        boolean any = calls.findByTeamIdAndMonthIndex(teamId, month).stream().anyMatch(CallPick::isLevered);
        if (!any && !tickers.isEmpty()) {
            applyPicks(teamId, month, currentDirections(teamId, month, tickers), tickers.get(0));
        }
    }

    private Map<String, String> currentDirections(long teamId, int month, List<String> tickers) {
        Map<String, String> dirs = new HashMap<>();
        for (String ticker : tickers) {
            dirs.put(ticker, LONG);
        }
        for (CallPick pick : calls.findByTeamIdAndMonthIndex(teamId, month)) {
            dirs.put(pick.getTicker(), pick.getDirection());
        }
        return dirs;
    }

    private Map<String, String> randomCalls(List<String> tickers) {
        Map<String, String> dirs = new HashMap<>();
        for (String ticker : tickers) {
            dirs.put(ticker, random.nextBoolean() ? LONG : SHORT);
        }
        return dirs;
    }

    // Writes the directions and the 2x pick for every roster stock, updating rows in place.
    private void applyPicks(long teamId, int month, Map<String, String> dirs, String levered) {
        Map<String, CallPick> existing = new HashMap<>();
        for (CallPick pick : calls.findByTeamIdAndMonthIndex(teamId, month)) {
            existing.put(pick.getTicker(), pick);
        }
        List<String> tickers = new ArrayList<>(dirs.keySet());
        if (levered == null && !existing.isEmpty()) {
            levered = existing.values().stream().filter(CallPick::isLevered).map(CallPick::getTicker).findFirst().orElse(null);
        }
        for (String ticker : tickers) {
            boolean isLevered = ticker.equals(levered);
            CallPick pick = existing.get(ticker);
            if (pick == null) {
                calls.save(new CallPick(teamId, month, ticker, dirs.get(ticker), isLevered));
            } else {
                pick.set(dirs.get(ticker), isLevered);
                calls.save(pick);
            }
        }
    }

    private Map<String, Stock> stockByTicker() {
        Map<String, Stock> out = new HashMap<>();
        for (Stock s : stocks.findAll()) {
            out.put(s.getTicker(), s);
        }
        return out;
    }

    private CallSheetView view(Team team, int month, boolean own) {
        League current = league.league();
        Optional<CallSheet> sheet = sheets.findByTeamIdAndMonthIndex(team.getId(), month);
        Map<String, DailyPrice> latest = latestPrices();
        Map<String, Stock> byTicker = stockByTicker();
        List<CallRow> rows = new ArrayList<>();
        String leveredTicker = null;
        for (CallPick pick : calls.findByTeamIdAndMonthIndex(team.getId(), month)) {
            Stock stock = byTicker.get(pick.getTicker());
            DailyPrice price = stock == null ? null : latest.get(pick.getTicker());
            rows.add(new CallRow(pick.getTicker(), stock == null ? "" : stock.getName(),
                    stock == null ? "" : stock.getSector(), pick.getDirection(), pick.isLevered(),
                    price == null ? null : price.getClosePrice()));
            if (pick.isLevered()) {
                leveredTicker = pick.getTicker();
            }
        }
        rows.sort(Comparator.comparing(CallRow::ticker));
        boolean submitted = sheet.map(CallSheet::isSubmitted).orElse(false);
        return new CallSheetView(month, label(month), own && isOpen(current, month) && !submitted,
                submitted, sheet.map(CallSheet::getNote).orElse(null), leveredTicker, rows);
    }

    private Map<String, DailyPrice> latestPrices() {
        Map<String, DailyPrice> out = new HashMap<>();
        Map<Integer, String> tickerById = new HashMap<>();
        for (Stock s : stocks.findAll()) {
            tickerById.put(s.getId(), s.getTicker());
        }
        for (DailyPrice price : prices.findLatestForEachStock()) {
            String ticker = tickerById.get(price.getStockId());
            if (ticker != null) {
                out.put(ticker, price);
            }
        }
        return out;
    }

    public record CallRow(String ticker, String name, String sector, String direction, boolean levered, BigDecimal price) {
    }

    public record CallSheetView(int month, String label, boolean editable, boolean submitted, String note,
                                String levered, List<CallRow> picks) {
    }
}
