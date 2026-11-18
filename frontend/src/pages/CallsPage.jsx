import { useEffect, useState } from 'react';
import useLeague from '../hooks/useLeague.js';
import { getCalls, saveCalls, submitCalls } from '../services/leagueService.js';
import { formatPrice } from '../utils/format.js';

// The month whose calls you set now: the first month before the season, otherwise the month after the live one.
function defaultMonth(summary) {
  switch (summary.status) {
    case 'PRESEASON':
      return 0;
    case 'IN_SEASON':
      return Math.min(summary.currentMonth + 1, 2);
    case 'COMPLETE':
      return 2;
    default:
      return 0;
  }
}

// "Submitted <timestamp>" reads as "Submitted" in the header; other notes are shown as they are.
function lockNote(note) {
  if (!note) return '';
  return note.startsWith('Submitted') ? '' : ` · ${note}`;
}

// Long or short on each roster stock, plus one 2x pick. Calls for a month are set during the month
// before it and lock when it starts; a month you do not submit carries over last month's calls.
function CallsPage({ token, onUnauthorized }) {
  const { summary, refresh, isLoading } = useLeague(token, onUnauthorized);
  const [month, setMonth] = useState(null);
  const [sheet, setSheet] = useState(null);
  const [directions, setDirections] = useState({});
  const [levered, setLevered] = useState(null);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const [isBusy, setIsBusy] = useState(false);

  useEffect(() => {
    if (summary && month === null) {
      setMonth(defaultMonth(summary));
    }
  }, [summary, month]);

  useEffect(() => {
    if (month === null || !summary?.myTeam) {
      return undefined;
    }
    let cancelled = false;
    getCalls(month, token)
      .then((next) => {
        if (cancelled) return;
        setSheet(next);
        // Every roster stock starts as Long, the same default the screen shows, unless the server has a call for it.
        const saved = Object.fromEntries(next.picks.map((p) => [p.ticker, p.direction]));
        setDirections(Object.fromEntries(summary.myTeam.roster.map((r) => [r.ticker, saved[r.ticker] ?? 'LONG'])));
        setLevered(next.levered);
        setError(null);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      });
    return () => {
      cancelled = true;
    };
  }, [month, token, summary?.myTeam]); // eslint-disable-line react-hooks/exhaustive-deps

  if (isLoading && !summary) {
    return <p className="loading">Loading your calls…</p>;
  }
  if (!summary?.myTeam) {
    return <p className="empty">Calls are for league members. {summary?.myRequest ? 'Your join request is pending.' : 'Join the league first.'}</p>;
  }

  const roster = summary.myTeam.roster;
  const editable = Boolean(sheet?.editable);
  const priceOf = (ticker) => roster.find((r) => r.ticker === ticker)?.price;

  async function run(action, success) {
    setIsBusy(true);
    setError(null);
    setMessage(null);
    try {
      const next = await action();
      setSheet(next);
      setMessage(success);
      await refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setIsBusy(false);
    }
  }

  return (
    <div className="calls">
      <div className="tabs" role="tablist" aria-label="Month">
        {summary.monthLabels.map((label, index) => (
          <button
            key={label}
            type="button"
            role="tab"
            aria-selected={month === index}
            className={`tab ${month === index ? 'is-active' : ''}`}
            onClick={() => setMonth(index)}
          >
            {label}
          </button>
        ))}
      </div>

      <section className="panel">
        <div className="panel__head">
          <h2>{summary.monthLabels[month] ?? ''} calls</h2>
          {sheet && (
            <span className={`pill ${editable ? 'pill--open' : 'pill--locked'}`}>
              {editable ? 'Open for editing' : sheet.submitted ? `Locked${lockNote(sheet.note)}` : 'Locked'}
            </span>
          )}
        </div>
        {!editable && sheet && (
          <p className="muted">
            {sheet.submitted ? 'These calls are final.' : 'This month is not open for calls.'}
          </p>
        )}

        <div className="table-wrap">
          <table className="calls-table">
            <thead>
              <tr>
                <th>Ticker</th>
                <th>Company</th>
                <th className="num">Close</th>
                <th>Call</th>
                <th className="center">2x</th>
              </tr>
            </thead>
            <tbody>
              {roster.map((row) => {
                const dir = directions[row.ticker] ?? 'LONG';
                return (
                  <tr key={row.ticker}>
                    <td className="mono strong">{row.ticker}</td>
                    <td>{row.name}</td>
                    <td className="num">{formatPrice(priceOf(row.ticker)) || '—'}</td>
                    <td>
                      {editable ? (
                        <div className="segmented" role="group" aria-label={`Call for ${row.ticker}`}>
                          {['LONG', 'SHORT'].map((option) => (
                            <button
                              key={option}
                              type="button"
                              aria-pressed={dir === option}
                              className={dir === option ? 'is-on' : ''}
                              onClick={() => setDirections((cur) => ({ ...cur, [row.ticker]: option }))}
                            >
                              {option === 'LONG' ? 'Long' : 'Short'}
                            </button>
                          ))}
                        </div>
                      ) : (
                        <span className={`pill ${dir === 'LONG' ? 'pill--long' : 'pill--short'}`}>
                          {dir === 'LONG' ? 'Long' : 'Short'}
                        </span>
                      )}
                    </td>
                    <td className="center">
                      <input
                        type="radio"
                        name="levered"
                        aria-label={`2x on ${row.ticker}`}
                        disabled={!editable}
                        checked={levered === row.ticker}
                        onChange={() => setLevered(row.ticker)}
                      />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {editable && (
          <div className="actions">
            <button
              type="button"
              className="button"
              disabled={isBusy}
              onClick={() => run(() => saveCalls(month, directions, levered, token), 'Draft saved.')}
            >
              Save draft
            </button>
            <button
              type="button"
              className="button button--primary"
              disabled={isBusy || !levered}
              onClick={() => run(() => saveCalls(month, directions, levered, token).then(() => submitCalls(month, token)), 'Calls submitted and locked in.')}
            >
              Submit calls
            </button>
            {!levered && <span className="muted small">Pick one 2x call to submit.</span>}
          </div>
        )}
        {message && <p className="notice" role="status">{message}</p>}
        {error && <p className="notice notice--error" role="alert">{error}</p>}
      </section>
    </div>
  );
}

export default CallsPage;
