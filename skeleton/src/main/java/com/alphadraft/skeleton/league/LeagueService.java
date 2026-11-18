package com.alphadraft.skeleton.league;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// League settings, player requests, and the admin's accept/reject decisions.
@Service
public class LeagueService {

    public static final int MIN_SIZE = 6;
    public static final int MAX_SIZE = 10;

    private final LeagueRepository leagues;
    private final TeamRepository teams;
    private final LeagueAuditRepository audit;
    private final Clock clock;

    public LeagueService(LeagueRepository leagues, TeamRepository teams, LeagueAuditRepository audit, Clock clock) {
        this.leagues = leagues;
        this.teams = teams;
        this.audit = audit;
        this.clock = clock;
    }

    public League league() {
        return leagues.findById(League.ID).orElseThrow(() -> new IllegalStateException("The league has not been created."));
    }

    public Optional<Team> teamOf(String email) {
        return teams.findByOwnerEmailIgnoreCase(email);
    }

    public List<Team> teamsWithStatus(TeamStatus status) {
        return teams.findByStatusOrderByRequestedAtAsc(status);
    }

    // Accepted players and bots: the teams that play the season.
    public List<Team> seatedTeams() {
        return teams.findByStatusIn(List.of(TeamStatus.ACCEPTED, TeamStatus.BOT));
    }

    public List<LeagueAudit> recentAudit() {
        return audit.findTop50ByOrderByAtDesc();
    }

    @Transactional
    public Team join(String email, String teamName) {
        League league = league();
        requireStatus(league, LeagueStatus.REGISTRATION, "The league is not taking sign-ups right now.");
        String name = teamName == null ? "" : teamName.trim();
        if (name.isEmpty()) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, "Give your team a name.");
        }
        if (name.length() > 40) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, "Team names are 40 characters or fewer.");
        }
        if (teamOf(email).isPresent()) {
            throw new LeagueException(HttpStatus.CONFLICT, "You already have a team in this league.");
        }
        Instant now = clock.instant();
        String color = BotTeams.PLAYER_COLORS.get((int) (teams.count() % BotTeams.PLAYER_COLORS.size()));
        Team team = teams.save(new Team(name, color, email.toLowerCase(), TeamStatus.PENDING, false, now));
        log("JOIN", name + " asked to join the league");
        return team;
    }

    @Transactional
    public Team accept(long teamId) {
        League league = league();
        requireStatus(league, LeagueStatus.REGISTRATION, "Players can only be accepted before the draft.");
        Team team = findTeam(teamId);
        if (team.getStatus() != TeamStatus.PENDING) {
            throw new LeagueException(HttpStatus.CONFLICT, team.getName() + " is not waiting for a decision.");
        }
        long accepted = teams.findByStatusIn(List.of(TeamStatus.ACCEPTED)).size();
        if (accepted >= league.getLeagueSize()) {
            throw new LeagueException(HttpStatus.CONFLICT,
                    "The league is full at " + league.getLeagueSize() + " teams. Raise the league size first.");
        }
        team.setStatus(TeamStatus.ACCEPTED);
        log("ACCEPT", team.getName() + " joined the league");
        return team;
    }

    @Transactional
    public Team reject(long teamId) {
        requireStatus(league(), LeagueStatus.REGISTRATION, "Players can only be turned down before the draft.");
        Team team = findTeam(teamId);
        if (team.getStatus() != TeamStatus.PENDING) {
            throw new LeagueException(HttpStatus.CONFLICT, team.getName() + " is not waiting for a decision.");
        }
        team.setStatus(TeamStatus.REJECTED);
        log("REJECT", team.getName() + " was turned down");
        return team;
    }

    @Transactional
    public League updateSettings(String name, int leagueSize, Instant draftAt, String firstMonth) {
        return updateSettings(name, leagueSize, draftAt, firstMonth, 90);
    }

    @Transactional
    public League updateSettings(String name, int leagueSize, Instant draftAt, String firstMonth, int pickSeconds) {
        if (pickSeconds < 30 || pickSeconds > 600) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, "A pick has between 30 seconds and 10 minutes.");
        }
        League league = league();
        requireStatus(league, LeagueStatus.REGISTRATION, "Settings can only change before the draft.");
        if (name == null || name.isBlank()) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, "The league needs a name.");
        }
        if (leagueSize < MIN_SIZE || leagueSize > MAX_SIZE) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, "A league has " + MIN_SIZE + " to " + MAX_SIZE + " teams.");
        }
        long accepted = teams.findByStatusIn(List.of(TeamStatus.ACCEPTED)).size();
        if (accepted > leagueSize) {
            throw new LeagueException(HttpStatus.CONFLICT,
                    accepted + " players are already accepted. Choose a size of at least " + accepted + ".");
        }
        if (draftAt != null && !draftAt.isAfter(clock.instant())) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, "Pick a draft time in the future.");
        }
        YearMonth first = parseMonth(firstMonth);
        league.setName(name.trim());
        league.setLeagueSize(leagueSize);
        league.setDraftAt(draftAt);
        league.setFirstMonth(first.toString());
        league.setPickSeconds(pickSeconds);
        log("SETTINGS", "League \"" + league.getName() + "\": " + leagueSize + " teams, " + pickSeconds
                + " seconds a pick, draft " + (draftAt == null ? "started by hand" : "at " + draftAt)
                + ", season from " + first);
        return league;
    }

    @Transactional
    public void log(String action, String detail) {
        audit.save(new LeagueAudit(clock.instant(), action, detail));
    }

    public static YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value == null ? "" : value.trim());
        } catch (RuntimeException e) {
            throw new LeagueException(HttpStatus.BAD_REQUEST, "The first month of the season must look like 2026-12.");
        }
    }

    public static void requireStatus(League league, LeagueStatus expected, String message) {
        if (league.getStatus() != expected) {
            throw new LeagueException(HttpStatus.CONFLICT, message);
        }
    }

    private Team findTeam(long teamId) {
        return teams.findById(teamId).orElseThrow(() -> new LeagueException(HttpStatus.NOT_FOUND, "No such team."));
    }
}
