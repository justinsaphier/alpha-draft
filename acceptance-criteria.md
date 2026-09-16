# Alpha Draft — Acceptance Criteria

Source: SCRUM-3 (Create Acceptance Criteria for Each User Story). Extracted from `alpha_draft_requirements.md`. Each user story below has 2-3 Given/When/Then conditions.

**1. As Marcus, I want to draft a 10-stock roster — 6 sector-designated starting spots and 4 bench spots — with no duplicate tickers league-wide, so that my team has both required category coverage and depth to work with.**
- Given it is Marcus's turn during rounds 1–6, when he selects a stock from that round's eligible starting-spot sector(s), then the stock fills that starting spot on his roster.
- Given it is Marcus's turn during rounds 7–10, when he selects any undrafted stock regardless of sector, then it is added to one of his 4 bench spots.
- Given Marcus has completed all 10 rounds, when the system checks his roster, then it confirms 6 starting spots and 4 bench spots are filled, with no ticker duplicated anywhere in the league.

**2. As Marcus, I want to set my active lineup each week by choosing which of my 10 stocks fills each of my 6 starting spots, so that I can bench an underperformer or rotate in a stock I like better.**
- Given Marcus owns a bench stock whose sector matches a starting spot's eligibility, when he swaps it into that spot for the week, then the previously-starting stock moves to the bench and does not score that week.
- Given Marcus attempts to place a stock into a starting spot whose sector it doesn't match, when he submits the lineup, then the system rejects the swap and displays an error.
- Given the weekly lock deadline passes, when Marcus's lineup is checked, then it is fixed for that week's scoring and cannot be edited further.

**3. As Marcus, I want to submit a long or short call for each of my 6 active starting stocks every week, so that my score reflects my current read on the market.**
- Given it is before the weekly lock deadline, when Marcus selects long or short for an active starting stock, then the call is saved and shown as his current selection.
- Given the weekly lock deadline has passed, when Marcus attempts to change a call, then the system prevents the edit and displays a "locked" message.
- Given Marcus has submitted calls for some but not all active spots, when he opens the weekly call screen, then the system shows which spots still need a call.

**4. As Marcus, I want to designate one of my six active spots as a levered (2x) pick each week, so that I can bet bigger on my highest-conviction call.**
- Given Marcus is submitting his weekly calls, when he selects an active spot as his levered pick, then that spot is marked 2x for the week.
- Given Marcus selects a different spot as levered after already designating one, when he saves the change, then only the newly selected spot carries the 2x multiplier.
- Given the week's scores are calculated, when the levered spot's return is applied, then it is doubled — positive or negative — before being added to his weekly total.

**5. As Priya, I want to compare two stocks in my roster side-by-side, so that I can make a more informed lineup or long/short decision each week.**
- Given Priya is on the weekly call screen, when she selects two stocks from her roster to compare, then both are displayed in parallel with sector and recent price history.
- Given Priya has selected only one stock, when she opens the comparison view, then the system prompts her to select a second stock.
- Given Priya closes the comparison view, when she returns to the weekly call screen, then her in-progress lineup and long/short selections are unchanged.

**6. As Priya, I want to propose a trade of one or more of my stocks to another player, so that I can improve my roster's sector coverage or acquire a stock I believe in.**
- Given Priya selects one or more of her owned stocks and one or more of another player's stocks, when she submits the trade proposal, then it is sent to the receiving player marked as pending.
- Given a proposed trade would leave either player unable to field at least one sector-eligible stock for every starting category, when Priya submits it, then the system rejects the proposal and explains why.
- Given Priya has a pending proposal awaiting response, when she views her trades, then she can see its status and cancel it before the other player responds.

**7. As Marcus, I want to accept or reject a trade offer sent to me, so that I control what leaves my roster.**
- Given Marcus has a pending trade offer, when he accepts it, then ownership of the traded stocks swaps between his and the proposing player's rosters, effective the next unlocked week.
- Given Marcus rejects a trade offer, when he submits the rejection, then no stocks change ownership and the proposing player is notified.
- Given a trade is accepted after the current week has already locked, when the next week begins, then both rosters reflect the new ownership for lineup-setting purposes.

**8. As Priya, I want to submit a waiver claim to drop one of my stocks and add an available one, so that I can improve my roster with a stock nobody currently owns.**
- Given Priya selects a stock she owns to drop and an available (undrafted or previously-dropped) stock to add, when she submits the claim, then it is queued as pending for that week's waiver processing.
- Given the claim would leave her roster unable to fill a required starting category, when she submits it, then the system rejects it immediately and explains why.
- Given the weekly waiver processing runs, when Priya's claim is evaluated, then the stock is awarded to her only if no higher-priority pending claim targets the same stock.

**9. As the system, I want waiver claims to resolve automatically in rotating priority order once a week, so that contested free-agent stocks are distributed fairly without manual intervention.**
- Given multiple pending claims target the same available stock, when weekly waiver processing runs, then the claim from the player with the highest current priority is awarded and the others are rejected.
- Given a player's claim is successfully awarded, when priority updates after processing, then that player moves to the back of the priority order.
- Given a player's claim fails because the stock was awarded to a higher-priority claim, when they check their claim status, then it is marked rejected and their roster is unchanged.

**10. As Marcus, I want to see a live price for each stock in my roster, so that I can gauge how my positions are doing before the week locks and scores are finalized.**
- Given Marcus opens his roster or dashboard, when the system fetches a live quote for each holding, then the current price is displayed alongside the stock.
- Given a live-quote request fails or times out, when Marcus views his roster, then the system shows the last cached price with a "live price unavailable" note instead of an error.
- Given live quotes are for display only, when weekly scores are calculated, then they use the cached weekly closing price from `price_snapshots`, never the live quote.

**11. As Marcus, I want to view the season leaderboard, so that I can see how my cumulative score compares to the rest of the league.**
- Given the current week's scores have been finalized, when Marcus views the leaderboard, then all players in the league (6–10) are displayed ranked by cumulative season score.
- Given Marcus selects his own name on the leaderboard, when the detail view loads, then his weekly score history is displayed.
- Given a new week's scores are calculated, when the leaderboard refreshes, then rankings update to reflect the new cumulative totals.

**12. As Jordan, I want the system to pull each stock's closing price from a market-data API every week, so that scores can be calculated without manual data entry for every stock.**
- Given Jordan triggers the weekly price refresh, when the system calls the pricing API for every stock in the pool, then each stock's closing price for that week is stored in `price_snapshots`.
- Given the API fails to return a price for a stock, when the refresh completes, then that stock is flagged for Jordan's review.
- Given `price_snapshots` has been populated for the week, when any player's score is calculated, then the system reads only from `price_snapshots`, never a live API call or live quote.

**13. As Jordan, I want to manually override or backfill a stock's price when the API fails or returns bad data, so that scoring stays accurate even when the data source has a problem.**
- Given a stock is flagged after a price refresh, when Jordan opens the flagged list, then he can see which stocks are missing or have suspect prices.
- Given Jordan enters a corrected closing price for a flagged stock, when he saves it, then the price is stored and marked as manually sourced.
- Given a price was manually entered, when any user views that stock's price detail, then the source is visibly labeled "Admin" rather than "API."

**14. As Jordan, I want to manage the stock pool and sector assignments, so that the draft and waiver wire always have an accurate, available set of stocks.**
- Given Jordan is on the stock pool management screen, when he adds a new stock and assigns it a sector, then it becomes eligible for any starting spot matching that sector, and for the bench and waiver pool.
- Given a stock has already been drafted, when Jordan attempts to edit or remove it, then the system prevents the change.
- Given Jordan updates a stock's sector before it's drafted, when he saves the change, then the draft board and any relevant lineup-swap eligibility reflect the update immediately.
