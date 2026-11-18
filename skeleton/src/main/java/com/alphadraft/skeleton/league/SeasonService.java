package com.alphadraft.skeleton.league;

import com.alphadraft.skeleton.model.DailyPrice;
import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.DailyPriceRepository;
import com.alphadraft.skeleton.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// The quarter: starts the season, closes each month from the price feed, scores every team, and
// lets an admin correct a price. Month -1 is the season's starting price; month m is the price at
// the end of month m, which is also the start price of month m+1.
@Service
public class SeasonService {

    public record StandingRow(int rank, long teamId, String name, String color, boolean bot,
                              List<Double> monthly, Double live, double total, double best) {
    }

    private static final Logger log = LoggerFactory.getLogger(SeasonService.class);

    public record LiveRow(String ticker, String name, String direction, boolean levered, BigDecimal startPrice,
                          BigDecimal livePrice, double returnPct, double points) {
    }

    private final LeagueService league;
    private final LeagueNotifications notifications;
    private final CallService calls;
    private final CallPickRepository callPicks;
    private final DraftPickRepository draftPicks;
    private final PriceSnapshotRepository snapshots;
    private final DailyPriceRepository prices;
    private final StockRepository stocks;

    public SeasonService(LeagueService league, LeagueNotifications notifications, CallService calls,
                         CallPickRepository callPicks, DraftPickRepository draftPicks,
                         PriceSnapshotRepository snapshots, DailyPriceRepository prices, StockRepository stocks) {
        this.league = league;
        this.notifications = notifications;
        this.calls = calls;
        this.callPicks = callPicks;
        this.draftPicks = draftPicks;
        this.snapshots = snapshots;
        this.prices = prices;
        this.stocks = stocks;
    }

    @Transactional
    public void startSeason() {
        League current = league.league();
        LeagueService.requireStatus(current, LeagueStatus.PRESEASON, "The season can start once the draft is over.");
        LeagueService.parseMonth(current.getFirstMonth());
        current.setStatus(LeagueStatus.IN_SEASON);
        current.setCurrentMonth(0);
        Map<Integer, DailyPrice> latest = latestByStockId();
        for (RosterStock stock : rosterStocks()) {
            PriceSnapshot start = snapshots.findByStockIdAndMonthIndex(stock.stockId(), -1)
                    .orElseGet(() -> new PriceSnapshot(stock.stockId(), stock.ticker(), -1, null));
            DailyPrice price = latest.get(stock.stockId());
            if (start.effective() == null && price != null) {
                start.setApiClose(price.getClosePrice());
            }
            snapshots.save(start);
        }
        calls.lockMonth(0);
        league.log("SEASON", "Season started. " + calls.label(0) + " is live.");
    }

    // Closes the live month: stores each drafted stock's month-end price, scores it, and opens the next month.
    @Transactional
    public void finalizeMonth() {
        League current = league.league();
        LeagueService.requireStatus(current, LeagueStatus.IN_SEASON, "No month is live.");
        int month = current.getCurrentMonth();
        Map<Integer, DailyPrice> latest = latestByStockId();
        List<String> missing = new ArrayList<>();
        for (RosterStock stock : rosterStocks()) {
            PriceSnapshot snap = snapshots.findByStockIdAndMonthIndex(stock.stockId(), month)
                    .orElseGet(() -> new PriceSnapshot(stock.stockId(), stock.ticker(), month, null));
            if (snap.effective() == null) {
                DailyPrice price = latest.get(stock.stockId());
                if (price == null) {
                    missing.add(stock.ticker());
                    continue;
                }
                snap.setApiClose(price.getClosePrice());
            }
            snapshots.save(snap);
        }
        if (!missing.isEmpty()) {
            throw new LeagueException(HttpStatus.CONFLICT, "No closing price yet for " + String.join(", ", missing)
                    + ". Refresh prices or enter them by hand, then finalize again.");
        }
        int next = month + 1;
        if (next >= League.MONTHS_IN_SEASON) {
            current.setStatus(LeagueStatus.COMPLETE);
            league.log("SCORE", calls.label(month) + " is final. The season is complete.");
        } else {
            current.setCurrentMonth(next);
            calls.lockMonth(next);
            league.log("SCORE", calls.label(month) + " is final. " + calls.label(next) + " is live.");
        }
        notifyMonthScored(month, next < League.MONTHS_IN_SEASON ? calls.label(next) : null);
    }

    // Closes the live month by itself once it has ended and every drafted stock has a recent price.
    // Returns false (and changes nothing) while prices are missing or stale, so the next run tries again.
    @Transactional
    public boolean finalizeIfDue(LocalDate today) {
        League current = league.league();
        if (current.getStatus() != LeagueStatus.IN_SEASON) {
            return false;
        }
        YearMonth live = LeagueService.parseMonth(current.getFirstMonth()).plusMonths(current.getCurrentMonth());
        if (!YearMonth.from(today).isAfter(live)) {
            return false;
        }
        LocalDate fresh = live.atEndOfMonth().minusDays(3);
        Map<Integer, DailyPrice> latest = latestByStockId();
        for (RosterStock stock : rosterStocks()) {
            DailyPrice price = latest.get(stock.stockId());
            if (price == null || price.getTradeDate().isBefore(fresh)) {
                log.warn("Month not closed automatically: {} has no current price yet", stock.ticker());
                return false;
            }
        }
        finalizeMonth();
        return true;
    }

    // Each player's calls for the live month, priced from the latest close against the month's start.
    public List<LiveRow> liveBreakdown(long teamId) {
        League current = league.league();
        if (current.getStatus() != LeagueStatus.IN_SEASON) {
            return List.of();
        }
        int month = current.getCurrentMonth();
        Map<String, PriceSnapshot> index = snapshotIndex();
        Map<Integer, DailyPrice> live = latestByStockId();
        Map<String, RosterStock> byTicker = new HashMap<>();
        for (RosterStock stock : rosterStocksFor(teamId)) {
            byTicker.put(stock.ticker(), stock);
        }
        List<LiveRow> rows = new ArrayList<>();
        for (CallPick pick : callPicks.findByTeamIdAndMonthIndex(teamId, month)) {
            RosterStock stock = byTicker.get(pick.getTicker());
            if (stock == null) {
                continue;
            }
            PriceSnapshot start = index.get(key(stock.stockId(), month - 1));
            BigDecimal startPrice = start == null ? null : start.effective();
            DailyPrice latestPrice = live.get(stock.stockId());
            BigDecimal livePrice = latestPrice == null ? null : latestPrice.getClosePrice();
            double ret = 0;
            double points = 0;
            if (startPrice != null && livePrice != null && startPrice.signum() != 0) {
                ret = livePrice.doubleValue() / startPrice.doubleValue() - 1;
                points = Scoring.points(ret, pick.getDirection(), pick.isLevered());
            }
            rows.add(new LiveRow(stock.ticker(), stock.name(), pick.getDirection(), pick.isLevered(),
                    startPrice, livePrice, ret * 100, points));
        }
        rows.sort(java.util.Comparator.comparing(LiveRow::ticker));
        return rows;
    }

    private void notifyMonthScored(int month, String nextLabel) {
        Map<Long, StandingRow> rows = new HashMap<>();
        for (StandingRow row : standings()) {
            rows.put(row.teamId(), row);
        }
        for (Team team : league.seatedTeams()) {
            StandingRow row = rows.get(team.getId());
            if (!team.isBot() && row != null) {
                notifications.monthScored(team, calls.label(month), row.monthly().get(month), nextLabel);
            }
        }
    }

    // Enters an admin's correction for a drafted stock's price in a month that has started.
    @Transactional
    public void overridePrice(String ticker, int month, BigDecimal close) {
        League current = league.league();
        if (current.getStatus() != LeagueStatus.IN_SEASON && current.getStatus() != LeagueStatus.COMPLETE) {
            throw new LeagueException(HttpStatus.CONFLICT, "Prices can be corrected once the season has started.");
        }
        int lastMonth = current.getStatus() == LeagueStatus.COMPLETE ? League.MONTHS_IN_SEASON - 1 : current.getCurrentMonth();
        if (month < -1 || month > lastMonth) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, "That month has not started yet.");
        }
        if (close == null || close.signum() <= 0) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, "The price must be a positive number.");
        }
        RosterStock stock = rosterStock(ticker);
        PriceSnapshot snap = snapshots.findByStockIdAndMonthIndex(stock.stockId(), month)
                .orElseGet(() -> new PriceSnapshot(stock.stockId(), stock.ticker(), month, null));
        snap.setManualClose(close.setScale(4, RoundingMode.HALF_UP));
        snapshots.save(snap);
        league.log("OVERRIDE", stock.ticker() + " " + calls.label(month) + " close set by hand to $" + close.setScale(2, RoundingMode.HALF_UP));
    }

    @Transactional
    public void clearOverride(String ticker, int month) {
        RosterStock stock = rosterStock(ticker);
        PriceSnapshot snap = snapshots.findByStockIdAndMonthIndex(stock.stockId(), month)
                .orElseThrow(() -> new LeagueException(HttpStatus.NOT_FOUND, "There is no price to clear for " + stock.ticker() + "."));
        if (!snap.isManual()) {
            throw new LeagueException(HttpStatus.CONFLICT, stock.ticker() + " has no price set by hand for that month.");
        }
        snap.setManualClose(null);
        snapshots.save(snap);
        league.log("OVERRIDE", stock.ticker() + " " + calls.label(month) + " reverted to the price feed");
    }

    public int finalizedMonths(League current) {
        return switch (current.getStatus()) {
            case COMPLETE -> League.MONTHS_IN_SEASON;
            case IN_SEASON -> current.getCurrentMonth();
            default -> 0;
        };
    }

    public List<StandingRow> standings() {
        League current = league.league();
        int finalized = finalizedMonths(current);
        Map<String, PriceSnapshot> snapshotIndex = snapshotIndex();
        Map<Integer, DailyPrice> live = latestByStockId();

        List<StandingRow> rows = new ArrayList<>();
        for (Team team : league.seatedTeams()) {
            List<Double> monthly = new ArrayList<>();
            for (int m = 0; m < finalized; m++) {
                monthly.add(points(team.getId(), m, snapshotIndex, null));
            }
            Double liveTotal = null;
            if (current.getStatus() == LeagueStatus.IN_SEASON) {
                liveTotal = points(team.getId(), current.getCurrentMonth(), snapshotIndex, live);
            }
            double total = monthly.stream().mapToDouble(Double::doubleValue).sum();
            double best = monthly.isEmpty() ? 0 : monthly.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            rows.add(new StandingRow(0, team.getId(), team.getName(), team.getColor(), team.isBot(), monthly, liveTotal, total, best));
        }
        rows.sort(Comparator.comparingDouble(StandingRow::total).reversed().thenComparing(Comparator.comparingDouble(StandingRow::best).reversed()));
        List<StandingRow> ranked = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            StandingRow r = rows.get(i);
            ranked.add(new StandingRow(i + 1, r.teamId(), r.name(), r.color(), r.bot(), r.monthly(), r.live(), r.total(), r.best()));
        }
        return ranked;
    }

    // The price table for the admin: each drafted stock's starting price and month-end prices.
    public List<PriceRow> priceTable() {
        League current = league.league();
        Map<String, PriceSnapshot> index = snapshotIndex();
        Map<Integer, DailyPrice> live = latestByStockId();
        List<PriceRow> rows = new ArrayList<>();
        for (RosterStock stock : rosterStocks()) {
            Map<Integer, PriceCell> cells = new LinkedHashMap<>();
            for (int m = -1; m < League.MONTHS_IN_SEASON; m++) {
                PriceSnapshot snap = index.get(key(stock.stockId(), m));
                if (snap != null) {
                    cells.put(m, new PriceCell(snap.effective(), snap.isManual()));
                }
            }
            DailyPrice latest = live.get(stock.stockId());
            rows.add(new PriceRow(stock.ticker(), stock.name(), latest == null ? null : latest.getClosePrice(), cells));
        }
        return rows;
    }

    // Drafted stocks with no price from the feed yet.
    public List<String> missingPrices() {
        Map<Integer, DailyPrice> latest = latestByStockId();
        return rosterStocks().stream().filter(s -> !latest.containsKey(s.stockId())).map(RosterStock::ticker).toList();
    }

    public record PriceCell(BigDecimal price, boolean manual) {
    }

    public record PriceRow(String ticker, String name, BigDecimal livePrice, Map<Integer, PriceCell> months) {
    }

    // Points a team scored in month m: finished months use the month-end prices; the live month
    // (when livePrices is given) uses today's price against the start of the month.
    private double points(long teamId, int month, Map<String, PriceSnapshot> snapshotIndex, Map<Integer, DailyPrice> livePrices) {
        Map<String, RosterStock> byTicker = new LinkedHashMap<>();
        for (RosterStock stock : rosterStocksFor(teamId)) {
            byTicker.put(stock.ticker(), stock);
        }
        double total = 0;
        for (CallPick pick : callPicks.findByTeamIdAndMonthIndex(teamId, month)) {
            RosterStock stock = byTicker.get(pick.getTicker());
            if (stock == null) {
                continue;
            }
            PriceSnapshot start = snapshotIndex.get(key(stock.stockId(), month - 1));
            BigDecimal startPrice = start == null ? null : start.effective();
            BigDecimal endPrice;
            if (livePrices != null) {
                DailyPrice live = livePrices.get(stock.stockId());
                endPrice = live == null ? null : live.getClosePrice();
            } else {
                PriceSnapshot end = snapshotIndex.get(key(stock.stockId(), month));
                endPrice = end == null ? null : end.effective();
            }
            if (startPrice == null || endPrice == null || startPrice.signum() == 0) {
                continue;
            }
            double ret = endPrice.doubleValue() / startPrice.doubleValue() - 1;
            total += Scoring.points(ret, pick.getDirection(), pick.isLevered());
        }
        return total;
    }

    private Map<String, PriceSnapshot> snapshotIndex() {
        Map<String, PriceSnapshot> index = new HashMap<>();
        for (PriceSnapshot snap : snapshots.findAll()) {
            index.put(key(snap.getStockId(), snap.getMonthIndex()), snap);
        }
        return index;
    }

    private static String key(int stockId, int month) {
        return stockId + "|" + month;
    }

    private Map<Integer, DailyPrice> latestByStockId() {
        Map<Integer, DailyPrice> out = new HashMap<>();
        for (DailyPrice price : prices.findLatestForEachStock()) {
            out.put(price.getStockId(), price);
        }
        return out;
    }

    public record RosterStock(int stockId, String ticker, String name) {
    }

    // Every stock that has been drafted, once each.
    private List<RosterStock> rosterStocks() {
        Map<Integer, RosterStock> out = new LinkedHashMap<>();
        Map<Integer, String> names = namesById();
        for (DraftPick pick : draftPicks.findAllByOrderByOverallAsc()) {
            if (pick.getStockId() != null) {
                out.putIfAbsent(pick.getStockId(), new RosterStock(pick.getStockId(), pick.getTicker(),
                        names.getOrDefault(pick.getStockId(), pick.getTicker())));
            }
        }
        return new ArrayList<>(out.values());
    }

    private List<RosterStock> rosterStocksFor(long teamId) {
        List<RosterStock> out = new ArrayList<>();
        Map<Integer, String> names = namesById();
        for (DraftPick pick : draftPicks.findAllByOrderByOverallAsc()) {
            if (pick.getTeamId() == teamId && pick.getStockId() != null) {
                out.add(new RosterStock(pick.getStockId(), pick.getTicker(),
                        names.getOrDefault(pick.getStockId(), pick.getTicker())));
            }
        }
        return out;
    }

    private Map<Integer, String> namesById() {
        Map<Integer, String> out = new HashMap<>();
        for (Stock stock : stocks.findAll()) {
            out.put(stock.getId(), stock.getName());
        }
        return out;
    }

    private RosterStock rosterStock(String ticker) {
        String wanted = ticker == null ? "" : ticker.trim();
        return rosterStocks().stream()
                .filter(s -> s.ticker().equalsIgnoreCase(wanted))
                .findFirst()
                .orElseThrow(() -> new LeagueException(HttpStatus.BAD_REQUEST, wanted + " is not drafted, so it is not scored."));
    }
}
