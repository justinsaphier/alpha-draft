# Alpha Draft — Requirements Document
Per course requirements: Personas, User Stories, Acceptance Criteria (Given/When/Then), Use Cases, and MVP definition.
# Personas

# User Stories

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


# MVP Definition