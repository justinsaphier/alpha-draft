import { useEffect, useState } from 'react';
import useLeague from '../hooks/useLeague.js';
import { getDraft, pickStock } from '../services/leagueService.js';
import { formatCountdown } from '../utils/time.js';

const POLL_MS = 4000;

// The snake draft. When it is your turn, you pick one stock from this round's sector group.
// Bots pick on their own turns. The board refreshes on its own while the draft runs.
function DraftPage({ token, onUnauthorized }) {
  const { summary, refresh, isLoading, error } = useLeague(token, onUnauthorized);
  const [draft, setDraft] = useState(null);
  const [draftError, setDraftError] = useState(null);
  const [filter, setFilter] = useState('');
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

  if (isLoading && !summary) {
    return <p className="loading">Loading the draft room…</p>;
  }
  if (error && !summary) {
    return <p role="alert" className="notice notice--error">Could not load the draft: {error}</p>;
  }
  if (!summary) {
    return null;
  }
  if (!summary.myTeam && summary.status !== 'REGISTRATION') {
    return <p className="empty">Only league members can see the draft board.</p>;
  }

  async function handlePick(ticker) {
    setIsPicking(true);
    setDraftError(null);
    try {
      setDraft(await pickStock(ticker, token));
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

  return (
    <div className="draft">
      <section className="hero">
        <div>
          <p className="eyebrow">Draft room</p>
          <h1>{statusTitle(summary.status)}</h1>
          <p className="muted">
            {running && draft?.myTurn && 'It is your pick. Choose a stock from this round’s sector group.'}
            {running && !draft?.myTurn && draft?.onTheClock && `On the clock: ${draft.onTheClock}.`}
            {summary.status === 'REGISTRATION' && 'The draft has not started. The admin sets the draft day.'}
            {summary.status !== 'REGISTRATION' && !running && summary.status !== 'DRAFTING' && 'Draft complete.'}
          </p>
          {running && draft?.onTheClockUntil && <Countdown until={draft.onTheClockUntil} onTheClock={draft.onTheClock} />}
        </div>
      </section>

      {draftError && <p className="notice notice--error" role="alert">{draftError}</p>}

      {running && draft?.myTurn && (
        <section className="panel">
          <div className="panel__head">
            <h2>Your pick</h2>
            <input
              aria-label="Filter stocks"
              placeholder="Filter by ticker or company"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            />
          </div>
          <ul className="eligible">
            {eligible.map((stock) => (
              <li key={stock.ticker}>
                <span className="mono strong">{stock.ticker}</span>
                <span className="grow">{stock.name}</span>
                <span className="muted small">{stock.sector}</span>
                <button
                  type="button"
                  className="button button--primary"
                  disabled={isPicking}
                  onClick={() => handlePick(stock.ticker)}
                >
                  Draft
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className="panel">
        <h2>Draft board</h2>
        <div className="table-wrap">
          <table className="board">
            <thead>
              <tr>
                <th>Pick</th>
                <th>Round</th>
                <th>Team</th>
                <th>Spot</th>
                <th>Ticker</th>
              </tr>
            </thead>
            <tbody>
              {(draft?.picks ?? []).map((p) => (
                <tr key={p.overall} className={draft?.currentOverall === p.overall ? 'is-clock' : ''}>
                  <td>{p.overall}</td>
                  <td>{p.round}</td>
                  <td>
                    <span className="dot" style={{ background: p.teamColor }} aria-hidden="true" />
                    {p.teamName}
                  </td>
                  <td>{p.spotLabel}</td>
                  <td className="mono strong">{p.ticker ?? '—'}</td>
                </tr>
              ))}
              {(!draft || draft.picks.length === 0) && (
                <tr>
                  <td colSpan={5} className="empty">The draft board appears when the draft starts.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

// Time left on the current pick. When it runs out the league picks for the player automatically.
function Countdown({ until, onTheClock }) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  const left = new Date(until).getTime() - now;
  return (
    <p className={`countdown ${left <= 10_000 ? 'is-urgent' : ''}`} role="timer" aria-live="off">
      {left > 0
        ? `${onTheClock} has ${formatCountdown(left)} to pick.`
        : `Time is up. A stock is being picked for ${onTheClock}.`}
    </p>
  );
}

function statusTitle(status) {
  switch (status) {
    case 'REGISTRATION':
      return 'Waiting for the draft';
    case 'DRAFTING':
      return 'Draft in progress';
    default:
      return 'Draft room';
  }
}

export default DraftPage;
