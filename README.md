# Alpha Draft

A fantasy-sports-style platform for stock picking. Six to ten players draft a sector-balanced, duplicate-free roster of six stocks, then re-commit every month to a long or short call on each holding — plus one 2x-levered pick. Scores are computed from real closing prices, and a season-long leaderboard tracks who called it best.

Built as a Software Studio backend project — full CRUD, two-role authentication, and a real database. Price data comes from a market-data API, but only as a scheduled backend ingestion job: the live app never calls the API in real time, so nothing external is on the critical path during a demo.

## Why This Project

Everyone has opinions about which stocks are overvalued or undervalued, but there's no easy, low-stakes way to actually test those calls against friends. Alpha Draft turns that into a season-long competition with real structure: you can't just pick your six favorite tech names and coast, and you can't set a roster once and forget about it — every month forces a fresh long/short decision, plus a bet on which one call you believe in most.

## League Format

- **6–10 players per league**, configurable when the league is created.  
- **6-round snake draft** — draft order is randomized once at season start, then reverses each round.  
- **6 roster spots**, one filled per round:  
  1. **Technology** — its own dedicated spot, since it's the largest sector by market weight in the S\&P 500\.  
  2. **Financials / Real Estate**  
  3. **Healthcare / Consumer Staples**  
  4. **Energy / Materials**  
  5. **Industrials / Consumer Discretionary**  
  6. **Utilities / Communication Services**

Each round is restricted to that round's eligible sector(s), and a stock is removed from the pool league-wide the moment anyone drafts it — no duplicate tickers.

## How It Works

### 1\. The Draft

Players fill their six roster spots one round at a time, snake-style, choosing from that round's eligible sector pool. A completed roster satisfies all six spots with no duplicate tickers anywhere in the league.

### 2\. Monthly Long/Short Calls \+ Levered Pick

The roster is fixed for the season. Each month, every player:

- Chooses **long or short** for each of their six holdings.  
- Designates **exactly one** of the six as that month's **levered pick** — its return counts double (2x), win or lose.

Calls lock once the month begins.

### 3\. Pricing & Scoring

- A scheduled ingestion job pulls each stock's closing price from a market-data API (see [Pricing Source](#pricing-source) below) and stores it in the app's own `price_snapshots` table.  
- All scoring and leaderboard logic reads only from that stored data — never live from the API — so a network issue or rate limit never affects the running app or a live demo.  
- Monthly team score \= sum across all six spots of `(± return) × leverage`, where the sign flips for a short and the levered spot gets a 2x multiplier.

## Pricing Source

Price data is fetched via a market-data API (Twelve Data or Finnhub are the leading candidates — both comfortably handle a one-shot monthly refresh of the full \~120–150 stock pool within their free-tier limits; Alpha Vantage's free tier is too rate-limited for a single-batch refresh at this scale).

- **Ingestion, not live lookup.** Admin (or a scheduled task) triggers the monthly refresh; results are cached in `price_snapshots` and reused for every score/leaderboard calculation.  
- **Admin override fallback.** If the API fails to return a price for a stock, or returns something clearly wrong, Admin can manually enter or correct that stock's closing price. Manual entries are flagged as `source: manual` versus `source: api` so the team can always see where a price came from.  
- **Adjusted close.** The ingestion job should request split/dividend-adjusted closing prices where the API supports it, so a stock split mid-month doesn't read as a huge (and fake) price move.  
- **API key handling.** Store the key in a `.env` file, gitignored — never commit it to the shared repo.

## Roles & Permissions

| Role | Can do |
| :---- | :---- |
| **Admin** | Manage the stock pool and sector assignments, trigger the monthly price refresh job, manually override/backfill a price when the API fails, manage league/season settings |
| **Player** | Draft stocks (within league rules), submit monthly long/short calls and levered-pick selection (until locked), view leaderboard and own roster history |

The two-role split is a real authentication requirement, not just a feature: Admin's job didn't disappear when pricing became API-driven — it shifted from manual data entry to managing the pool, triggering ingestion, and handling exceptions.

## Data Model (proposed)

| Table | Purpose | Key fields |
| :---- | :---- | :---- |
| `sectors` | Sector reference list | `id`, `name` |
| `stocks` | Draftable ticker pool, tagged to a roster spot | `id`, `ticker`, `name`, `sector_id`, `roster_spot`, `is_drafted` |
| `users` | Accounts | `id`, `email`, `password_hash`, `role` (admin/player) |
| `leagues` | A season/league instance | `id`, `name`, `player_count` (6–10), `roster_size` (6) |
| `draft_picks` | Who drafted what, and in which round/spot | `id`, `league_id`, `user_id`, `stock_id` (unique), `roster_spot`, `pick_number` |
| `monthly_positions` | Each player's call per stock per month | `id`, `user_id`, `stock_id`, `month`, `direction` (long/short), `is_levered`, `locked_at` |
| `price_snapshots` | Closing prices, API-sourced or admin-overridden | `id`, `stock_id`, `month`, `close_price`, `source` (api/manual) |
| `monthly_scores` | Cached computed scores (optional but recommended) | `id`, `user_id`, `month`, `score` |

Full CRUD lives most clearly on `monthly_positions` (create/read/update/delete a call before lock) and on Admin's management of `stocks` and `price_snapshots`.

## Suggested Tech Stack

- **Backend:** Flask \+ SQLAlchemy (ORM makes the sector/spot validation and scoring logic much cleaner than raw SQL)  
- **Database:** SQLite for local dev, Postgres if deploying somewhere persistent  
- **Auth:** Flask-Login or Flask-JWT-Extended for the two-role system  
- **Price ingestion:** A Python script/module using `requests` against the chosen API, triggered by an Admin-facing endpoint (simplest) or a scheduled job (APScheduler or a cron-triggered script, if the team wants full automation)  
- **Frontend:** Jinja templates for a fast MVP, or a lightweight React frontend if the team wants a more modern client  
- **Dev environment:** VS Code, with a shared `.vscode/` config (recommended extensions, launch config) so everyone's debugging setup matches  
- **Version control:** GitHub, trunk-based with short-lived feature branches per Jira ticket; `.env` (API keys) gitignored

## Suggested Task Breakdown

**Auth & Roles**

- Set up user model with Admin/Player roles  
- Registration/login flow  
- Route-level permission checks (Admin-only endpoints)

**League & Draft Engine**

- League/season creation (Admin), configurable 6–10 player capacity  
- Stock pool \+ sector/roster-spot setup (Admin)  
- 6-round snake draft pick flow, one round per roster spot  
- No-duplicate validation across the league  
- Draft completion check (all 6 spots filled)

**Monthly Positions**

- Submit long/short call per stock  
- Select one levered (2x) pick per month  
- Lock mechanism (no edits after month start)  
- View own roster \+ position history

**Pricing & Ingestion**

- Integrate chosen market-data API client  
- Monthly price refresh job → `price_snapshots`  
- Flag stocks missing a price after refresh  
- Admin manual override/backfill flow

**Scoring & Leaderboard**

- Scoring calculation (per-stock return, long/short sign flip, leverage multiplier)  
- Season leaderboard view/API  
- Historical monthly breakdown per player

**Frontend / UX**

- Draft board UI (round-by-round, spot-restricted)  
- Monthly call \+ levered-pick submission UI  
- Leaderboard \+ roster views  
- Admin console (stock pool, price refresh/override)

**QA & Deployment**

- Unit tests on scoring \+ validation logic, especially the leverage multiplier and short sign flip (highest priority — this is what a grader will poke at)  
- Seed data / pre-loaded demo season so the leaderboard looks real on demo day  
- Deployment (or a reliable local demo setup)

## Open Decisions To Finalize

A few rules the team should settle explicitly before they come up mid-sprint:

- **Tie-breaker** for identical leaderboard scores.  
- **Corporate actions** — what happens if a held stock is acquired or delisted mid-season.  
- **Price-entry immutability** — should a finalized month's prices be locked/audit-logged rather than freely editable.
