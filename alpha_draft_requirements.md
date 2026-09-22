# Alpha Draft — Requirements Document
Per course requirements: Personas, User Stories, Acceptance Criteria (Given/When/Then), Use Cases, and MVP definition.
# Personas


# Use Cases:
## Use Case 1: Draft a Roster Actor: Marcus (Player) Precondition: Marcus is logged in, the league's draft has started, and it is his turn to pick. Steps:

Marcus views the available stock pool for the current round, filtered to that round's eligible sector(s).
Marcus selects a stock to draft.
System checks that the stock hasn't already been taken by any player in the league.
System adds the stock to Marcus's roster in the round's designated spot and advances the draft to the next player.
The draft continues snake-style through all 6 rounds until every player's roster is full.

Postcondition: Marcus's roster contains one stock in each of the 6 spots, with no duplicate tickers anywhere in the league.

Alternative flow: If Marcus attempts to select a stock that was just drafted by someone else (a timing conflict), the system rejects the pick, displays an error, and prompts him to choose a different stock from the current pool.

## Use Case 2: Submit Monthly Long/Short Calls and Levered Pick Actor: Priya (Player) Precondition: Priya is logged in, has a completed roster from the draft, and the current month's call window is open. Steps:

Priya views her roster at the start of the month.
She compares two stocks she's unsure about using the comparison tool.
She selects long or short for each stock she holds.
She designates one holding as her levered pick for the month.
She submits her calls before the lock deadline.

Postcondition: Priya's long/short calls and levered pick are saved and locked once the month begins, and are used in that month's scoring.

Alternative flow: If Priya attempts to submit after the lock deadline has passed, the system rejects the submission, displays a "calls are locked for this month" message, and her last saved calls (or a default) are used instead.

## Use Case 3: Refresh Monthly Prices & Generate Scores Actor: Jordan (Admin) Precondition: Jordan is logged in with Admin privileges, and the current month has ended. Steps:

Jordan triggers the monthly price refresh.
System calls the market-data API for every stock in the pool and stores the results in price_snapshots.
System flags any stock the API didn't return a usable price for.
Jordan manually enters a corrected price for each flagged stock.
System calculates every player's score — applying the long/short sign and the 2x leverage multiplier on each player's levered pick — and updates the season leaderboard.

Postcondition: Every stock in the pool has a recorded closing price for the month, every player has an updated monthly and cumulative score, and the leaderboard reflects the new totals.

Alternative flow: If the pricing API is completely unreachable, Jordan manually enters closing prices for the entire pool instead of relying on the automated refresh, and the system proceeds with scoring normally once all prices are recorded.


**MVP Goal:** This MVP lets a 6–10 player league compete over a full fiscal quarter on stock-picking skill within a structured, rule-bound game: a sector-balanced, 10-stock draft (6 starters plus 4 bench), a weekly lineup and long/short decision (plus one 2x-levered bet) on every active holding, player-to-player trading and waiver-wire pickups to reshape a roster mid-season, live quotes to track positions in real time, and a leaderboard built from real, cached closing-price data. Live prices inform the player; only the weekly snapshot ever determines a score — so nothing about the live app's correctness depends on a real-time external call succeeding. The MVP shows who called the market best over a quarter; it does not provide live trading, real-money stakes, or any predictive or advisory output.

##User Story 1. As Marcus, I want to draft a roster across all 6 roster spots with no duplicate tickers league-wide, so that my team is built within league rules.

Acceptance Criteria:

Given it is Marcus's turn in the draft, when he selects a stock from the current round's eligible sector pool, then that stock is added to his roster in the corresponding spot.
Given a stock has already been drafted by any player in the league, when Marcus attempts to select it, then the system rejects the selection and displays an error.
Given Marcus has completed all 6 rounds, when the system checks his roster, then it confirms all 6 spots are filled with no duplicate tickers.

##User Story 2. As Marcus, I want to submit a long or short call for each stock I hold every month, so that my score reflects my current read on the market.

Acceptance Criteria:

Given it is before the monthly lock deadline, when Marcus selects long or short for a stock in his roster, then the call is saved and shown as his current selection.
Given the monthly lock deadline has passed, when Marcus attempts to change a call, then the system prevents the edit and displays a "locked" message.
Given Marcus has submitted calls for some but not all holdings, when he opens the monthly call screen, then the system shows which holdings still need a call.

##User Story 3. As Marcus, I want to designate one of my six holdings as a levered (2x) pick each month, so that I can bet bigger on my highest-conviction call.

Acceptance Criteria:

Given Marcus is submitting his monthly calls, when he selects a roster spot as his levered pick, then that spot is marked 2x for the month.
Given Marcus has already designated a levered pick, when he selects a different spot as levered, then the system moves the designation and removes it from the previous spot (only one levered pick is active at a time).
Given the month's scores are calculated, when the levered spot's return is applied, then it is doubled — positive or negative — before being added to his monthly total.

##User Story 4. As Priya, I want to compare two stocks in my roster side-by-side, so that I can make a more informed long/short decision each month.

Acceptance Criteria:

Given Priya is on the monthly call screen, when she selects two stocks from her roster to compare, then both are displayed in parallel with sector and recent price history.
Given Priya has selected only one stock, when she opens the comparison view, then the system prompts her to select a second stock.
Given Priya closes the comparison view, when she returns to the monthly call screen, then her in-progress long/short selections are unchanged.

