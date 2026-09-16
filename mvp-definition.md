# Alpha Draft — MVP Definition

Source: SCRUM-5 (Create MVP Definition). Extracted from `alpha_draft_requirements.md`.

## In Scope
- User accounts with two roles: Player and Admin
- 6–10 player league (configurable) with a 10-round snake draft
- 10-stock roster per player: 6 sector-designated starting spots (Technology dedicated, plus 5 paired-sector spots) and 4 unrestricted bench spots
- Weekly lineup management — swapping a bench stock into a starting spot, restricted to matching sector eligibility
- Weekly long/short call submission on the 6 active starting stocks, with locking after the deadline
- Weekly levered (2x) pick selection, one per player per week
- Player-to-player trading: propose, accept/reject, with validation preventing a trade that would leave a roster unable to fill a required starting category
- Waiver wire: drop a roster stock and claim an available (undrafted or dropped) stock, resolved weekly in rotating priority order, with the same starting-category validation used for trades
- Live/real-time quote display on a player's roster and dashboard — informational only, fetched on-demand or via periodic polling
- Automated weekly price ingestion via a market-data API, cached in the app's own database — the sole source of truth for official scoring
- Admin manual price override/backfill for API failures or bad data
- Automated score calculation per player, per week
- Season length: one fiscal quarter (~13 weeks)
- Season leaderboard and per-player score history
- Basic two-stock comparison tool

## Out of Scope
- Live/real-time prices used as the basis for official scoring — scoring always derives from the cached weekly `price_snapshots`, never a live quote; "live" here means an on-demand or short-interval polled price, not a streaming/tick-by-tick feed
- Real-money trading, deposits, or payouts
- Mobile app
- Multiple concurrent leagues per user in one season
- Automated sector classification (sectors are admin-curated, not pulled from a data provider)
- Notifications or reminders
- Social features beyond trade offers (no comments, chat, or direct messaging)
- Multi-season historical analytics

## MVP Goal
This MVP lets a 6–10 player league compete over a full fiscal quarter on stock-picking skill within a structured, rule-bound game: a sector-balanced, 10-stock draft (6 starters plus 4 bench), a weekly lineup and long/short decision (plus one 2x-levered bet) on every active holding, player-to-player trading and waiver-wire pickups to reshape a roster mid-season, live quotes to track positions in real time, and a leaderboard built from real, cached closing-price data. Live prices inform the player; only the weekly snapshot ever determines a score — so nothing about the live app's correctness depends on a real-time external call succeeding. The MVP shows who called the market best over a quarter; it does not provide live trading, real-money stakes, or any predictive or advisory output.
