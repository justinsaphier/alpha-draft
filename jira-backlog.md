# Alpha Draft — Jira Backlog (Epics, Stories, Tasks, Sprint Plan)

Paste this into Jira once the `atlassian` MCP connection is authenticated (or import manually). Ticket keys assume project key `PROJ` — update to match your actual Jira project key.

## Epic: PROJ-1 Auth & Roles
| Key | Type | Summary | Acceptance Criteria |
|---|---|---|---|
| PROJ-2 | Story | As a new user, I can register an account | Email/password stored hashed; role defaults to `player` |
| PROJ-3 | Story | As a user, I can log in and stay authenticated | Session/JWT issued on login; invalid creds rejected |
| PROJ-4 | Task | Create `User` model (id, email, password_hash, role) | Migration runs; unique email constraint |
| PROJ-5 | Task | Add route-level permission decorator (`admin_required`) | Non-admin hitting admin route gets 403 |
| PROJ-6 | Story | As Admin, I can access admin-only endpoints that players cannot | Player gets 403 on admin routes; Admin gets 200 |

## Epic: PROJ-7 League & Draft Engine
| Key | Type | Summary | Acceptance Criteria |
|---|---|---|---|
| PROJ-8 | Task | Create `League` model (name, player_count 6–10, roster_size=6) | Validation rejects player_count outside 6–10 |
| PROJ-9 | Task | Create `Stock` + `Sector` models, seed sector list | 6 sectors seeded matching roster spots |
| PROJ-10 | Story | As Admin, I can manage the stock pool and sector/roster-spot tags | CRUD on stocks; each stock tagged to exactly one roster spot |
| PROJ-11 | Story | As a Player, I can draft a stock in my current round's eligible sector | Pick rejected if stock's sector doesn't match round; pick rejected if already drafted league-wide |
| PROJ-12 | Task | Implement 6-round snake draft order/turn logic | Order randomized once per league; reverses each round |
| PROJ-13 | Task | Enforce no-duplicate-ticker validation league-wide | Second player attempting same ticker gets error |
| PROJ-14 | Task | Draft completion check (all 6 spots filled per player) | League marked "draft complete" only when every player has 6 picks |

## Epic: PROJ-15 Monthly Positions
| Key | Type | Summary | Acceptance Criteria |
|---|---|---|---|
| PROJ-16 | Story | As a Player, I can submit a long/short call per holding each month | One direction per stock per month; editable until lock |
| PROJ-17 | Story | As a Player, I can designate exactly one holding as my levered (2x) pick | Selecting a second levered pick replaces/rejects the first; exactly one enforced at lock |
| PROJ-18 | Task | Implement lock mechanism (no edits after month start) | PATCH after `locked_at` returns 409/423 |
| PROJ-19 | Story | As a Player, I can view my roster and position history | Endpoint/page lists all past months' calls |

## Epic: PROJ-20 Pricing & Ingestion
| Key | Type | Summary | Acceptance Criteria |
|---|---|---|---|
| PROJ-21 | Task | Integrate market-data API client (Twelve Data or Finnhub) | API key read from `.env`; client wrapper unit-testable/mockable |
| PROJ-22 | Task | Build monthly price refresh job → `price_snapshots` | Adjusted close stored per stock per month; `source=api` |
| PROJ-23 | Task | Flag stocks missing a price after refresh | Admin sees a list of unresolved/missing-price stocks |
| PROJ-24 | Story | As Admin, I can manually override/backfill a price when the API fails | Manual entry stored with `source=manual`; visible distinctly from API prices |

## Epic: PROJ-25 Scoring & Leaderboard
| Key | Type | Summary | Acceptance Criteria |
|---|---|---|---|
| PROJ-26 | Task | Implement scoring calc: sign flip for short, 2x for levered pick | Unit tests cover long/short × levered/non-levered (4 cases) |
| PROJ-27 | Story | As a Player, I can view the season leaderboard | Sorted by total score descending; ties handled per decided tie-breaker rule |
| PROJ-28 | Story | As a Player, I can view historical monthly breakdown per player | Shows month-by-month score contribution per holding |

## Epic: PROJ-29 Frontend / UX
| Key | Type | Summary | Acceptance Criteria |
|---|---|---|---|
| PROJ-30 | Story | As a Player, I see a draft board restricted to the current round's sector | Ineligible stocks shown disabled/hidden |
| PROJ-31 | Story | As a Player, I can submit my monthly calls + levered pick in one UI flow | Submit blocked if levered pick not selected |
| PROJ-32 | Story | As a Player, I can view leaderboard and roster pages | Matches backend data 1:1 |
| PROJ-33 | Story | As Admin, I have a console for stock pool mgmt and price refresh/override | Admin-only route; triggers ingestion job; shows override form |

## Epic: PROJ-34 QA & Deployment
| Key | Type | Summary | Acceptance Criteria |
|---|---|---|---|
| PROJ-35 | Task | Unit tests for scoring + validation logic (highest priority) | Covers leverage multiplier + short sign flip edge cases |
| PROJ-36 | Task | Seed data / demo season | `flask seed-demo` (or equivalent) populates a realistic leaderboard |
| PROJ-37 | Task | Deployment or reliable local demo setup | Documented steps reproduce a working demo from a clean clone |

## Decisions to resolve before sprint planning (blockers, not tickets)
- Tie-breaker rule for identical leaderboard scores
- Corporate actions handling (acquired/delisted stock mid-season)
- Price-entry immutability (lock/audit-log finalized month prices?)

## Suggested Sprint Breakdown (2-week sprints, adjust to team velocity)
- **Sprint 1:** Auth & Roles (PROJ-2–6) + League/Stock/Sector models (PROJ-8–9)
- **Sprint 2:** Draft engine (PROJ-10–14)
- **Sprint 3:** Monthly Positions (PROJ-16–19) + Pricing ingestion (PROJ-21–24)
- **Sprint 4:** Scoring & Leaderboard (PROJ-26–28) + Frontend core (PROJ-30–32)
- **Sprint 5:** Admin console (PROJ-33) + QA/Deployment (PROJ-35–37) + buffer for review rounds
