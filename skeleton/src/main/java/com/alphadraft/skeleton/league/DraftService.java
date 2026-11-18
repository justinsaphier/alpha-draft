package com.alphadraft.skeleton.league;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.StockRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// The snake draft: start it, let bots pick on their turn, and take a human's pick when it is their turn.
// Each round a team picks one stock from that round's sector group, and no ticker can be picked twice.
@Service
public class DraftService {

    private final LeagueService league;
    private final TeamRepository teams;
    private final DraftPickRepository picks;
    private final StockRepository stocks;
    private final Clock clock;
    private final Random random;

    public DraftService(LeagueService league, TeamRepository teams, DraftPickRepository picks,
                        StockRepository stocks, Clock clock, Random random) {
        this.league = league;
        this.teams = teams;
        this.picks = picks;
        this.stocks = stocks;
        this.clock = clock;
        this.random = random;
    }

    public List<DraftPick> board() {
        return picks.findAllByOrderByOverallAsc();
    }

    @Transactional
    public void startDraft() {
        League current = league.league();
        LeagueService.requireStatus(current, LeagueStatus.REGISTRATION,
                "The draft has already started, or the league is not open.");
        List<Team> accepted = teams.findByStatusIn(List.of(TeamStatus.ACCEPTED));
        if (accepted.isEmpty()) {
            throw new LeagueException(HttpStatus.CONFLICT, "Accept at least one player before the draft.");
        }
        if (accepted.size() > current.getLeagueSize()) {
            throw new LeagueException(HttpStatus.CONFLICT, "More players are accepted than the league has places for.");
        }

        List<Team> all = new ArrayList<>(accepted);
        int bots = current.getLeagueSize() - accepted.size();
        for (int i = 0; i < bots; i++) {
            BotTeams.Identity identity = BotTeams.BOTS.get(i);
            all.add(teams.save(new Team(identity.name(), identity.color(), null, TeamStatus.BOT, true, clock.instant())));
        }

        List<Long> order = new ArrayList<>(all.stream().map(Team::getId).toList());
        Collections.shuffle(order, random);
        for (SnakeDraft.Slot slot : SnakeDraft.order(order, RosterSpots.ALL.size())) {
            picks.save(new DraftPick(slot.overall(), slot.round(), slot.teamId(), slot.spotId()));
        }

        current.setStatus(LeagueStatus.DRAFTING);
        league.log("DRAFT", "Draft started with " + accepted.size() + " players and " + bots
                + " bots, in snake order");
        advanceBots();
    }

    @Transactional
    public void pick(String email, String ticker) {
        League current = league.league();
        if (current.getStatus() != LeagueStatus.DRAFTING) {
            throw new LeagueException(HttpStatus.CONFLICT, "The draft is not running.");
        }
        Team team = league.teamOf(email)
                .filter(t -> t.getStatus() == TeamStatus.ACCEPTED)
                .orElseThrow(() -> new LeagueException(HttpStatus.FORBIDDEN, "Only accepted players can draft."));
        DraftPick turn = nextPick().orElseThrow(() -> new LeagueException(HttpStatus.CONFLICT, "The draft is over."));
        if (turn.getTeamId() != team.getId()) {
            throw new LeagueException(HttpStatus.CONFLICT, "It is not your pick.");
        }
        Stock stock = findStock(ticker);
        if (RosterSpots.spotForSector(stock.getSector()) != turn.getSpotId()) {
            throw new LeagueException(HttpStatus.BAD_REQUEST,
                    stock.getTicker() + " is not eligible for this round (" + RosterSpots.labelFor(turn.getSpotId()) + ").");
        }
        if (takenStockIds().contains(stock.getId())) {
            throw new LeagueException(HttpStatus.CONFLICT, stock.getTicker() + " is already drafted.");
        }
        turn.make(stock.getId(), stock.getTicker(), clock.instant());
        picks.save(turn);
        league.log("PICK", team.getName() + " drafted " + stock.getTicker() + " in round " + turn.getRound());
        advanceBots();
    }

    // Bots pick until it is a human's turn or the draft is over.
    @Transactional
    public void advanceBots() {
        while (true) {
            Optional<DraftPick> next = nextPick();
            if (next.isEmpty()) {
                completeDraft();
                return;
            }
            Team team = teams.findById(next.get().getTeamId()).orElseThrow();
            if (!team.isBot()) {
                return;
            }
            Set<Integer> taken = takenStockIds();
            List<Stock> eligible = stocks.findAll().stream()
                    .filter(s -> RosterSpots.spotForSector(s.getSector()) == next.get().getSpotId())
                    .filter(s -> !taken.contains(s.getId()))
                    .toList();
            if (eligible.isEmpty()) {
                throw new IllegalStateException("No stock is left for spot " + next.get().getSpotId());
            }
            Stock choice = eligible.get(random.nextInt(eligible.size()));
            next.get().make(choice.getId(), choice.getTicker(), clock.instant());
            picks.save(next.get());
            league.log("PICK", team.getName() + " (bot) drafted " + choice.getTicker() + " in round " + next.get().getRound());
        }
    }

    private void completeDraft() {
        League current = league.league();
        current.setStatus(LeagueStatus.PRESEASON);
        current.setCurrentMonth(-1);
        league.log("DRAFT", "Draft complete. Calls for the first month are open.");
    }

    private Optional<DraftPick> nextPick() {
        return picks.findFirstByStockIdIsNullOrderByOverallAsc();
    }

    private Set<Integer> takenStockIds() {
        Set<Integer> taken = new HashSet<>();
        for (DraftPick pick : picks.findAll()) {
            if (pick.getStockId() != null) {
                taken.add(pick.getStockId());
            }
        }
        return taken;
    }

    private Stock findStock(String ticker) {
        String wanted = ticker == null ? "" : ticker.trim();
        return stocks.findAll().stream()
                .filter(s -> s.getTicker().equalsIgnoreCase(wanted))
                .findFirst()
                .orElseThrow(() -> new LeagueException(HttpStatus.BAD_REQUEST, "Unknown ticker: " + wanted));
    }
}
