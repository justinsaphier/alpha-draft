import { useEffect, useState } from 'react';
import useLeague from '../hooks/useLeague.js';
import StandingsTable from '../components/StandingsTable.jsx';
import { getTeamCalls } from '../services/leagueService.js';
import { formatPrice } from '../utils/format.js';

// Standings for the league. Pick a team to see its calls for a month (once that month has locked).
function LeaderboardPage({ token, onUnauthorized }) {
  const { summary, standings, error, isLoading } = useLeague(token, onUnauthorized);
  const [teamId, setTeamId] = useState(null);
  const [month, setMonth] = useState(0);
  const [sheet, setSheet] = useState(null);
  const [sheetError, setSheetError] = useState(null);

  useEffect(() => {
    if (teamId === null) {
      return undefined;
    }
    let cancelled = false;
    getTeamCalls(teamId, month, token)
      .then((next) => {
        if (!cancelled) {
          setSheet(next);
          setSheetError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setSheet(null);
          setSheetError(err.message);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [teamId, month, token]);

  if (isLoading && !summary) {
    return <p className="loading">Loading standings…</p>;
  }
  if (error && !summary) {
    return <p role="alert" className="notice notice--error">Could not load standings: {error}</p>;
  }
  if (!summary) {
    return null;
  }

  const selected = standings.find((row) => row.teamId === teamId);
  // Months you can open: finished months and the live one.
  const openable = summary.monthLabels.map((label, index) => ({ label, index })).filter((m) => m.index <= Math.max(summary.currentMonth, 0));

  return (
    <div className="leaderboard">
      <section className="panel">
        <h2>Standings</h2>
        <p className="muted small">Points are summed across each finished month. Click a team to see its calls.</p>
        <StandingsTable
          standings={standings}
          monthLabels={summary.monthLabels}
          selectedId={teamId}
          onSelect={setTeamId}
        />
      </section>

      {selected && (
        <section className="panel">
          <div className="panel__head">
            <h2>
              <span className="dot" style={{ background: selected.color }} aria-hidden="true" />
              {selected.name}
            </h2>
            <label className="field-inline">
              Month
              <select value={month} onChange={(e) => setMonth(Number(e.target.value))}>
                {openable.map((m) => (
                  <option key={m.index} value={m.index}>
                    {m.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
          {sheetError && <p className="notice">{sheetError}</p>}
          {sheet && (
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
                  {sheet.picks.map((p) => (
                    <tr key={p.ticker}>
                      <td className="mono strong">{p.ticker}</td>
                      <td>{p.name}</td>
                      <td className="num">{formatPrice(p.price) || '—'}</td>
                      <td>
                        <span className={`pill ${p.direction === 'LONG' ? 'pill--long' : 'pill--short'}`}>
                          {p.direction === 'LONG' ? 'Long' : 'Short'}
                        </span>
                      </td>
                      <td className="center">{p.levered ? '2x' : ''}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {selected.live != null && <p className="muted small">Live points this month: {selected.live.toFixed(1)}</p>}
        </section>
      )}
    </div>
  );
}

export default LeaderboardPage;
