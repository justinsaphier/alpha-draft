import { useEffect, useMemo, useState } from 'react';
import useLeague from '../hooks/useLeague.js';
import { getDraft, pickStock } from '../services/leagueService.js';
import { formatCountdown } from '../utils/time.js';

const POLL_MS = 4000;

// The snake draft. Bots pick on their own turns. When it is your turn you choose a stock from this
// round's sector group and confirm it. The board, the recent picks and your own roster update live.
function DraftPage({ token, onUnauthorized }) {
  const { summary, refresh, isLoading, error } = useLeague(token, onUnauthorized);
  const [draft, setDraft] = useState(null);
  const [draftError, setDraftError] = useState(null);
  const [filter, setFilter] = useState('');
  const [chosen, setChosen] = useState(null);
  const [isPicking, setIsPicking] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getDraft(token)
      .then((next) => {
        if (!cancelled) setDraft(next);
      })
      .catch((err) => {
        if (!cancelled) setDraftError(err.message);
      });
    return () => {
      cancelled = true;
    };
  }, [token, summary?.status]);

  const running = draft?.status === 'DRAFTING';
  useEffect(() => {
    if (!running) return undefined;
    const timer = setInterval(async () => {
      try {
        setDraft(await getDraft(token));
        await refresh();
      } catch {
        // A failed poll is retried on the next tick.
      }
    }, POLL_MS);
    return () => clearInterval(timer);
  }, [running, token, refresh]);

  // Columns are the teams in draft order (round 1 order); rows are the rounds.
  const { columns, rows, recent, mine, turnPick } = useMemo(() => {
    const picks = draft?.picks ?? [];
    const firstRound = picks.filter((p) => p.round === 1);
    const cols = firstRound.map((p) => ({ teamId: p.teamId, teamName: p.teamName, teamColor: p.teamColor }));
    const roundCount = picks.reduce((max, p) => Math.max(max, p.round), 0);
    const grid = [];
    for (let r = 1; r <= roundCount; r += 1) {
      grid.push({ round: r, cells: cols.map((c) => picks.find((p) => p.round === r && p.teamId === c.teamId) ?? null) });
    }
    const made = picks.filter((p) => p.ticker).sort((a, b) => b.overall - a.overall);
    const myName = summary?.myTeam?.name;
    return {
      columns: cols,
      rows: grid,
      recent: made.slice(0, 8),
      mine: myName ? picks.filter((p) => p.teamName === myName && p.ticker) : [],
      turnPick: picks.find((p) => p.overall === draft?.currentOverall) ?? null,
    };
  }, [draft, summary?.myTeam?.name]);

  if (isLoading && !summary) {
    return <p className="loading">Loading the draft room…</p>;
  }
  if (error && !summary) {
    return <p role="alert" className="notice notice--error">Could not load the draft: {error}</p>;
  }
  if (!summary) {
    return null;
  }
  if (!summary.myTeam && summary.status !== 'REGISTRATION' && summary.status !== 'DRAFTING') {
    return <p className="empty">Only league members can see the draft board.</p>;
  }

  async function confirmPick() {
    if (!chosen) return;
    setIsPicking(true);
    setDraftError(null);
    try {
      setDraft(await pickStock(chosen, token));
      setChosen(null);
      await refresh();
    } catch (err) {
      setDraftError(err.message);
    } finally {
      setIsPicking(false);
    }
  }

  const eligible = (draft?.eligible ?? []).filter(
    (s) => !filter || s.ticker.toLowerCase().includes(filter.toLowerCase()) || s.name.toLowerCase().includes(filter.toLowerCase()),
  );
  const onClock = draft?.onTheClock;

  return (
    <div className="draft">
      <section className={`clock-card ${draft?.myTurn ? 'is-mine' : ''}`} aria-live="polite">
        <div className="clock-card__main">
          <p className="eyebrow">{statusLabel(summary.status)}</p>
          <h1>
            {running && draft?.myTurn && 'It’s your pick'}
            {running && !draft?.myTurn && onClock && `On the clock: ${onClock}`}
            {summary.status === 'REGISTRATION' && 'Waiting for the draft'}
            {summary.status === 'PRESEASON' && 'Draft complete'}
            {summary.status === 'IN_SEASON' && 'Draft complete, season underway'}
            {summary.status === 'COMPLETE' && 'Season complete'}
          </h1>
          {running && turnPick && (
            <p className="muted">
              Round {turnPick.round}, pick {turnPick.overall} of {draft.picks.length} · {turnPick.spotLabel}
            </p>
          )}
          {summary.status === 'REGISTRATION' && (
            <p className="muted">The draft has not started. The admin sets the draft day.</p>
          )}
        </div>
        {running && draft?.onTheClockUntil && (
          <Countdown until={draft.onTheClockUntil} name={onClock} isMine={draft.myTurn} />
        )}
      </section>

      {draftError && <p className="notice notice--error" role="alert">{draftError}</p>}

      {running && draft?.myTurn && (
        <section className="panel">
          <div className="panel__head">
            <h2>Choose a stock</h2>
            <input
              aria-label="Filter stocks"
              placeholder="Search ticker or company"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            />
          </div>
          <p className="muted small">
            This round picks from {turnPick?.spotLabel}. Select a stock, then confirm it.
          </p>
          <ul className="eligible">
            {eligible.map((stock) => (
              <li key={stock.ticker} className={chosen === stock.ticker ? 'is-chosen' : ''}>
                <button
                  type="button"
                  className="eligible__pick"
                  aria-pressed={chosen === stock.ticker}
                  onClick={() => setChosen(stock.ticker)}
                >
                  <span className="mono strong">{stock.ticker}</span>
                  <span className="grow">{stock.name}</span>
                  <span className="muted small">{stock.sector}</span>
                </button>
              </li>
            ))}
            {eligible.length === 0 && <li className="muted">No stocks match.</li>}
          </ul>
          <div className="actions">
            <button type="button" className="button button--primary" disabled={!chosen || isPicking} onClick={confirmPick}>
              {chosen ? `Confirm ${chosen}` : 'Select a stock'}
            </button>
            {chosen && (
              <button type="button" className="button" disabled={isPicking} onClick={() => setChosen(null)}>
                Cancel
              </button>
            )}
          </div>
        </section>
      )}

      <div className="draft-grid">
        <section className="panel">
          <h2>Draft board</h2>
          {columns.length === 0 ? (
            <p className="empty">The board appears when the draft starts.</p>
          ) : (
            <div className="table-wrap">
              <table className="board-grid">
                <thead>
                  <tr>
                    <th>Round</th>
                    {columns.map((c) => (
                      <th key={c.teamId}>
                        <span className="dot" style={{ background: c.teamColor }} aria-hidden="true" />
                        {c.teamName}
                        {summary.myTeam?.name === c.teamName && <span className="badge">you</span>}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={row.round}>
                      <td className="muted">{row.round}</td>
                      {row.cells.map((cell, i) => {
                        const onTheClockCell = cell && cell.overall === draft?.currentOverall;
                        return (
                          <td
                            key={columns[i].teamId}
                            className={[
                              cell?.ticker ? 'cell--filled' : '',
                              onTheClockCell ? 'cell--clock' : '',
                              cell?.teamName === summary.myTeam?.name ? 'cell--mine' : '',
                            ].join(' ')}
                          >
                            {cell?.ticker ?? (cell ? '' : '—')}
                            <div className="muted small">{cell ? `#${cell.overall}` : ''}</div>
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <div className="stack">
          <section className="panel">
            <h2>Your roster so far</h2>
            {mine.length === 0 && <p className="muted">Your picks appear here.</p>}
            <ul className="list">
              {mine.map((p) => (
                <li key={p.overall}>
                  <span className="mono strong">{p.ticker}</span>
                  <span className="muted small grow">{p.spotLabel}</span>
                </li>
              ))}
            </ul>
          </section>
          <section className="panel">
            <h2>Latest picks</h2>
            {recent.length === 0 && <p className="muted">No picks yet.</p>}
            <ul className="audit">
              {recent.map((p) => (
                <li key={p.overall}>
                  <span className="muted small">#{p.overall}</span>
                  <span className="dot" style={{ background: p.teamColor }} aria-hidden="true" />
                  <span>{p.teamName}</span>
                  <span className="mono strong">{p.ticker}</span>
                </li>
              ))}
            </ul>
          </section>
        </div>
      </div>
    </div>
  );
}

// Time left on the current pick. When it runs out the league picks for the player automatically.
function Countdown({ until, name, isMine }) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  const left = new Date(until).getTime() - now;
  return (
    <div className={`clock ${left <= 10_000 ? 'is-urgent' : ''}`} role="timer" aria-label="Time left on this pick">
      <span className="clock__value">{left > 0 ? formatCountdown(left) : '0:00'}</span>
      <span className="muted small">
        {left > 0 ? (isMine ? 'to pick' : `${name} to pick`) : 'time is up, picking automatically'}
      </span>
    </div>
  );
}

function statusLabel(status) {
  switch (status) {
    case 'REGISTRATION':
      return 'Draft room';
    case 'DRAFTING':
      return 'Live draft';
    default:
      return 'Draft room';
  }
}

export default DraftPage;
