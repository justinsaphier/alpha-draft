# Alpha Draft — Requirements Document

Per course requirements: Personas, User Stories, Acceptance Criteria (Given/When/Then), Use Cases, and MVP definition.

## Personas

**Marcus, 21, Finance major.** He follows 15–20 stocks closely and already has strong opinions about which ones are over- or under-valued. He wants a competitive way to prove his calls are right against his friends, without putting real money on the line. He's on his laptop most of the day between classes and checks in from his phone otherwise. He's frustrated that fantasy sports apps never let him compete on the thing he's actually good at — reading the market.

**Priya, 20, Business major (undecided track).** She's newer to investing and doesn't have strong convictions about individual sectors yet. She wants enough context on screen — sector, recent price trend — that she doesn't feel lost drafting or making a weekly call next to more experienced friends. She mostly uses her phone and checks in between classes. She's frustrated that most investing tools assume she already knows what she's doing.

**Jordan, 22, League Commissioner (Admin).** He sets up and runs the league for his friend group each quarter. He wants the weekly price update to take a couple of minutes, not become a chore, so the league actually stays alive for the full season. He's frustrated when tools that are supposed to automate something quietly fail and he only finds out once someone complains their score looks wrong.

## User Stories & Acceptance Criteria

**1. As Marcus, I want to draft a 10-stock roster — 6 sector-designated starting spots and 4 bench spots — with no duplicate tickers league-wide, so that my team has both required category coverage and depth to work with.**

Acceptance Criteria:
- Given it is Marcus's turn during rounds 1–6, when he selects a stock from that round's eligible starting-spot sector(s), then the stock fills that starting spot on his roster.
- Given it is Marcus's turn during rounds 7–10, when he selects any undrafted stock regardless of sector, then it is added to one of his 4 bench spots.
- Given Marcus has completed all 10 rounds, when the system checks his roster, then it confirms 6 starting spots and 4 bench spots are filled, with no ticker duplicated anywhere in the league.

**2. As Marcus, I want to set my active lineup each week by choosing which of my 10 stocks fills each of my 6 starting spots, so that I can bench an underperformer or rotate in a stock I like better.**

Acceptance Criteria:
- Given Marcus owns a bench stock whose sector matches a starting spot's eligibility, when he swaps it into that spot for the week, then the previously-starting stock moves to the bench and does not score that week.
- Given Marcus attempts to place a stock into a starting spot whose sector it doesn't match, when he submits the lineup, then the system rejects the swap and displays an error.
- Given the weekly lock deadline passes, when Marcus's lineup is checked, then it is fixed for that week's scoring and cannot be edited further.

**3. As Marcus, I want to submit a long or short call for each of my 6 active starting stocks every week, so that my score reflects my current read on the market.**

Acceptance Criteria:
- Given it is before the weekly lock deadline, when Marcus selects long or short for an active starting stock, then the call is saved and shown as his current selection.
- Given the weekly lock deadline has passed, when Marcus attempts to change a call, then the system prevents the edit and displays a "locked" message.
- Given Marcus has submitted calls for some but not all active spots, when he opens the weekly call screen, then the system shows which spots still need a call.

**4. As Marcus, I want to designate one of my six active spots as a levered (2x) pick each week, so that I can bet bigger on my highest-conviction call.**

Acceptance Criteria:
- Given Marcus is submitting his weekly calls, when he selects an active spot as his levered pick, then that spot is marked 2x for the week.
- Given Marcus selects a different spot as levered after already designating one, when he saves the change, then only the newly selected spot carries the 2x multiplier.
- Given the week's scores are calculated, when the levered spot's return is applied, then it is doubled — positive or negative — before being added to his weekly total.

**5. As Priya, I want to compare two stocks in my roster side-by-side, so that I can make a more informed lineup or long/short decision each week.**

Acceptance Criteria:
- Given Priya is on the weekly call screen, when she selects two stocks from her roster to compare, then both are displayed in parallel with sector and recent price history.
- Given Priya has selected only one stock, when she opens the comparison view, then the system prompts her to select a second stock.
- Given Priya closes the comparison view, when she returns to the weekly call screen, then her in-progress lineup and long/short selections are unchanged.

**6. As Priya, I want to propose a trade of one or more of my stocks to another player, so that I can improve my roster's sector coverage or acquire a stock I believe in.**

Acceptance Criteria:
- Given Priya selects one or more of her owned stocks and one or more of another player's stocks, when she submits the trade proposal, then it is sent to the receiving player marked as pending.
- Given a proposed trade would leave either player unable to field at least one sector-eligible stock for every starting category, when Priya submits it, then the system rejects the proposal and explains why.
- Given Priya has a pending proposal awaiting response, when she views her trades, then she can see its status and cancel it before the other player responds.

**7. As Marcus, I want to accept or reject a trade offer sent to me, so that I control what leaves my roster.**

Acceptance Criteria:
- Given Marcus has a pending trade offer, when he accepts it, then ownership of the traded stocks swaps between his and the proposing player's rosters, effective the next unlocked week.
- Given Marcus rejects a trade offer, when he submits the rejection, then no stocks change ownership and the proposing player is notified.
- Given a trade is accepted after the current week has already locked, when the next week begins, then both rosters reflect the new ownership for lineup-setting purposes.

**8. As Priya, I want to submit a waiver claim to drop one of my stocks and add an available one, so that I can improve my roster with a stock nobody currently owns.**

Acceptance Criteria:
- Given Priya selects a stock she owns to drop and an available (undrafted or previously-dropped) stock to add, when she submits the claim, then it is queued as pending for that week's waiver processing.
- Given the claim would leave her roster unable to fill a required starting category, when she submits it, then the system rejects it immediately and explains why.
- Given the weekly waiver processing runs, when Priya's claim is evaluated, then the stock is awarded to her only if no higher-priority pending claim targets the same stock.

**9. As the system, I want waiver claims to resolve automatically in rotating priority order once a week, so that contested free-agent stocks are distributed fairly without manual intervention.**

Acceptance Criteria:
- Given multiple pending claims target the same available stock, when weekly waiver processing runs, then the claim from the player with the highest current priority is awarded and the others are rejected.
- Given a player's claim is successfully awarded, when priority updates after processing, then that player moves to the back of the priority order.
- Given a player's claim fails because the stock was awarded to a higher-priority claim, when they check their claim status, then it is marked rejected and their roster is unchanged.

**10. As Marcus, I want to see a live price for each stock in my roster, so that I can gauge how my positions are doing before the week locks and scores are finalized.**

Acceptance Criteria:
- Given Marcus opens his roster or dashboard, when the system fetches a live quote for each holding, then the current price is displayed alongside the stock.
- Given a live-quote request fails or times out, when Marcus views his roster, then the system shows the last cached price with a "live price unavailable" note instead of an error.
- Given live quotes are for display only, when weekly scores are calculated, then they use the cached weekly closing price from `price_snapshots`, never the live quote.

**11. As Marcus, I want to view the season leaderboard, so that I can see how my cumulative score compares to the rest of the league.**

Acceptance Criteria:
- Given the current week's scores have been finalized, when Marcus views the leaderboard, then all players in the league (6–10) are displayed ranked by cumulative season score.
- Given Marcus selects his own name on the leaderboard, when the detail view loads, then his weekly score history is displayed.
- Given a new week's scores are calculated, when the leaderboard refreshes, then rankings update to reflect the new cumulative totals.

**12. As Jordan, I want the system to pull each stock's closing price from a market-data API every week, so that scores can be calculated without manual data entry for every stock.**

Acceptance Criteria:
- Given Jordan triggers the weekly price refresh, when the system calls the pricing API for every stock in the pool, then each stock's closing price for that week is stored in `price_snapshots`.
- Given the API fails to return a price for a stock, when the refresh completes, then that stock is flagged for Jordan's review.
- Given `price_snapshots` has been populated for the week, when any player's score is calculated, then the system reads only from `price_snapshots`, never a live API call or live quote.

**13. As Jordan, I want to manually override or backfill a stock's price when the API fails or returns bad data, so that scoring stays accurate even when the data source has a problem.**

Acceptance Criteria:
- Given a stock is flagged after a price refresh, when Jordan opens the flagged list, then he can see which stocks are missing or have suspect prices.
- Given Jordan enters a corrected closing price for a flagged stock, when he saves it, then the price is stored and marked as manually sourced.
- Given a price was manually entered, when any user views that stock's price detail, then the source is visibly labeled "Admin" rather than "API."

**14. As Jordan, I want to manage the stock pool and sector assignments, so that the draft and waiver wire always have an accurate, available set of stocks.**

Acceptance Criteria:
- Given Jordan is on the stock pool management screen, when he adds a new stock and assigns it a sector, then it becomes eligible for any starting spot matching that sector, and for the bench and waiver pool.
- Given a stock has already been drafted, when Jordan attempts to edit or remove it, then the system prevents the change.
- Given Jordan updates a stock's sector before it's drafted, when he saves the change, then the draft board and any relevant lineup-swap eligibility reflect the update immediately.

## Use Cases

**Use Case 1: Draft a Roster**
Actor: Marcus (Player)
Precondition: Marcus is logged in, the league's draft has started, and it is his turn to pick.
Steps:
1. In rounds 1–6, Marcus views the eligible sector pool for the current round and selects a stock, filling that starting spot.
2. In rounds 7–10, Marcus views the full remaining pool (any sector) and selects a stock for one of his 4 bench spots.
3. System checks that the stock hasn't already been drafted by anyone in the league.
4. System adds the stock to the correct roster slot and advances the draft to the next player.
5. The draft continues snake-style through all 10 rounds until every player's roster is full.

Postcondition: Marcus's roster has 6 starting-spot stocks and 4 bench stocks, with no duplicate tickers anywhere in the league. Every undrafted stock enters the waiver pool, and initial waiver priority is set in reverse draft order.

Alternative flow: If Marcus attempts to select a stock that was just drafted by someone else, the system rejects the pick, displays an error, and prompts him to choose a different stock from the current pool.

**Use Case 2: Set Weekly Lineup and Submit Long/Short + Levered Pick**
Actor: Priya (Player)
Precondition: Priya is logged in, has a completed 10-stock roster, and the current week's call window is open.
Steps:
1. Priya views her 6 starting spots and 4 bench stocks, with live quotes shown alongside each, at the start of the week.
2. She optionally swaps a bench stock into a starting spot, if its sector matches that spot's eligibility.
3. She compares two stocks she's unsure about using the comparison tool.
4. She selects long or short for each of her 6 active starting stocks.
5. She designates one active spot as her levered pick and submits before the lock deadline.

Postcondition: Priya's lineup, long/short calls, and levered pick are saved and locked once the week begins, and are used in that week's scoring.

Alternative flow: If Priya attempts to submit after the lock deadline has passed, the system rejects the submission, displays a "calls are locked for this week" message, and her last saved lineup and calls are used instead.

**Use Case 3: Propose and Accept a Trade**
Actors: Priya (proposing player), Marcus (receiving player)
Precondition: Both Priya and Marcus are logged in, in the same league, and the season is still active.
Steps:
1. Priya selects one or more of her owned stocks and one or more of Marcus's stocks she wants in return.
2. System checks that the proposed trade would leave both rosters able to fill all 6 starting categories.
3. Priya submits the proposal; system notifies Marcus of a pending trade offer.
4. Marcus reviews the offer and accepts it.
5. System swaps ownership of the traded stocks between their rosters, effective the next unlocked week.

Postcondition: Both rosters reflect the new ownership, and each player's next weekly lineup can draw on the traded stocks.

Alternative flow: If Marcus rejects the offer instead, no stocks change ownership, and Priya is notified the trade was declined.

**Use Case 4: Submit and Resolve a Weekly Waiver Claim**
Actor: Priya (Player); System (automated weekly resolution)
Precondition: Priya is logged in, owns a roster, and the current week's waiver window is open.
Steps:
1. Priya views the list of available (undrafted or dropped) stocks.
2. She selects a stock to add and one of her own stocks to drop.
3. System checks the resulting roster would still satisfy all 6 starting-category requirements.
4. Priya submits the claim, which is queued as pending.
5. At the week's scheduled waiver-processing time, the system resolves all pending claims in priority order and updates rosters and the priority list accordingly.

Postcondition: Priya's roster reflects the swap if her claim was awarded; otherwise it is unchanged and she is notified the claim was unsuccessful.

Alternative flow: If two players claim the same available stock, the system awards it to whichever player currently holds higher waiver priority and rejects the other claim.

**Use Case 5: Refresh Weekly Prices & Generate Scores**
Actor: Jordan (Admin)
Precondition: Jordan is logged in with Admin privileges, and the current week has ended.
Steps:
1. Jordan triggers the weekly price refresh.
2. System calls the market-data API for every stock in the pool and stores the results in `price_snapshots` for that week.
3. System flags any stock the API didn't return a usable price for.
4. Jordan manually enters a corrected price for each flagged stock.
5. System calculates every player's score — applying the long/short sign and the 2x leverage multiplier on each player's levered pick — and updates the season leaderboard.

Postcondition: Every stock in the pool has a recorded closing price for the week, every player has an updated weekly and cumulative score, and the leaderboard reflects the new totals.

Alternative flow: If the pricing API is completely unreachable, Jordan manually enters closing prices for the entire pool instead of relying on the automated refresh, and the system proceeds with scoring normally once all prices are recorded.

## MVP Definition

**In Scope**
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

**Out of Scope**
- Live/real-time prices used as the basis for official scoring — scoring always derives from the cached weekly `price_snapshots`, never a live quote; "live" here means an on-demand or short-interval polled price, not a streaming/tick-by-tick feed
- Real-money trading, deposits, or payouts
- Mobile app
- Multiple concurrent leagues per user in one season
- Automated sector classification (sectors are admin-curated, not pulled from a data provider)
- Notifications or reminders
- Social features beyond trade offers (no comments, chat, or direct messaging)
- Multi-season historical analytics

**MVP Goal:** This MVP lets a 6–10 player league compete over a full fiscal quarter on stock-picking skill within a structured, rule-bound game: a sector-balanced, 10-stock draft (6 starters plus 4 bench), a weekly lineup and long/short decision (plus one 2x-levered bet) on every active holding, player-to-player trading and waiver-wire pickups to reshape a roster mid-season, live quotes to track positions in real time, and a leaderboard built from real, cached closing-price data. Live prices inform the player; only the weekly snapshot ever determines a score — so nothing about the live app's correctness depends on a real-time external call succeeding. The MVP shows who called the market best over a quarter; it does not provide live trading, real-money stakes, or any predictive or advisory output.

#User Story 5. As Marcus, I want to view the season leaderboard, so that I can see how my cumulative score compares to the rest of the league.

Acceptance Criteria:

Given the current month's scores have been finalized, when Marcus views the leaderboard, then all players in the league (6–10) are displayed ranked by cumulative season score.
Given Marcus selects his own name on the leaderboard, when the detail view loads, then his monthly score history is displayed.
Given a new month's scores are calculated, when the leaderboard refreshes, then rankings update to reflect the new cumulative totals.

#User Story 6. As Jordan, I want the system to pull each stock's closing price from a market-data API each month, so that scores can be calculated without manual data entry for every stock.

Acceptance Criteria:

Given Jordan triggers the monthly price refresh, when the system calls the pricing API for every stock in the pool, then each stock's closing price is stored in price_snapshots.
Given the API fails to return a price for a stock, when the refresh completes, then that stock is flagged for Jordan's review.
Given price_snapshots has been populated for the month, when any player's score is calculated, then the system reads only from price_snapshots, never a live API call.
