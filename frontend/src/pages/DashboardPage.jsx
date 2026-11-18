import { useEffect, useState } from 'react';
import useLeague from '../hooks/useLeague.js';
import { getLiveBreakdown, joinLeague } from '../services/leagueService.js';
import { formatPrice } from '../utils/format.js';
import { fmtPts, statusSentence } from '../utils/points.js';

// Your team at a glance: where you stand, your roster with live closes, and the league's teams.
function DashboardPage({ token, onUnauthorized }) {
  const { summary, standings, error, isLoading, refresh } = useLeague(token, onUnauthorized);
  const [teamName, setTeamName] = useState('');
  const [joinError, setJoinError] = useState(null);
  const [isJoining, setIsJoining] = useState(false);
  const [live, setLive] = useState([]);

  // This month's calls priced against today's closes; refreshed every minute while a month is live.
  const isLive = summary?.status === 'IN_SEASON' && Boolean(summary?.myTeam);
  useEffect(() => {
    if (!isLive) return undefined;
    let cancelled = false;
    const load = () =>
      getLiveBreakdown(token)
        .then((rows) => {
          if (!cancelled && Array.isArray(rows)) setLive(rows);
        })
        .catch(() => {});
    load();
    const timer = setInterval(load, 60_000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [isLive, token]);

  if (isLoading && !summary) {
    return <p className="loading">Loading your league…</p>;
  }
  if (error && !summary) {
    return <p role="alert" className="notice notice--error">Could not load the league: {error}</p>;
  }
  if (!summary) {
    return null;
  }

  const mine = summary.myTeam ? standings.find((row) => row.teamId === summary.myTeam.id) : null;

  async function handleJoin(event) {
    event.preventDefault();
    setIsJoining(true);
    setJoinError(null);
    try {
      await joinLeague(teamName, token);
      setTeamName('');
      await refresh();
    } catch (err) {
      setJoinError(err.message);
    } finally {
      setIsJoining(false);
    }
  }

  return (
    <div className="dashboard">
      <section className="hero">
        <div>
          <p className="eyebrow">{summary.name}</p>
          <h1>{summary.myTeam ? summary.myTeam.name : 'Welcome to the league'}</h1>
          <p className="muted">{statusSentence(summary)}</p>
        </div>
        {mine && (
          <div className="hero__stats">
            <Stat label="Rank" value={`#${mine.rank}`} />
            <Stat label="Season points" value={fmtPts(mine.total)} tone={mine.total >= 0 ? 'pos' : 'neg'} />
            <Stat label="Live month" value={mine.live == null ? '—' : fmtPts(mine.live)} tone={(mine.live ?? 0) >= 0 ? 'pos' : 'neg'} />
          </div>
        )}
      </section>

      {!summary.myTeam && summary.status === 'REGISTRATION' && !summary.myRequest && (
        <section className="panel">
          <h2>Join the league</h2>
          <p className="muted">Ask to join with a team name. The admin accepts each player before the draft.</p>
          <form className="inline-form" onSubmit={handleJoin}>
            <input
              aria-label="Team name"
              placeholder="Team name"
              value={teamName}
              maxLength={40}
              onChange={(e) => setTeamName(e.target.value)}
            />
            <button type="submit" className="button button--primary" disabled={isJoining}>
              {isJoining ? 'Sending…' : 'Request to join'}
            </button>
          </form>
          {joinError && <p className="notice notice--error" role="alert">{joinError}</p>}
        </section>
      )}

      {summary.myRequest === 'PENDING' && (
        <p className="notice">Your request to join is waiting for the admin.</p>
      )}
      {summary.myRequest === 'REJECTED' && (
        <p className="notice notice--error">The admin turned down your request to join this league.</p>
      )}

      {isLive && live.length > 0 && (
        <section className="panel">
          <div className="panel__head">
            <h2>{summary.monthLabels[summary.currentMonth]}, live</h2>
            <span className={`stat__value ${live.reduce((s, r) => s + r.points, 0) >= 0 ? 'pos' : 'neg'}`}>
              {fmtPts(live.reduce((s, r) => s + r.points, 0))} points
            </span>
          </div>
          <div className="table-wrap">
            <table className="standings">
              <thead>
                <tr>
                  <th>Ticker</th>
                  <th>Call</th>
                  <th className="num">Start</th>
                  <th className="num">Now</th>
                  <th className="num">Return</th>
                  <th className="num">Points</th>
                </tr>
              </thead>
              <tbody>
                {live.map((row) => (
                  <tr key={row.ticker}>
                    <td className="mono strong">
                      {row.ticker}
                      {row.levered && <span className="badge">2x</span>}
                    </td>
                    <td>
                      <span className={`pill ${row.direction === 'LONG' ? 'pill--long' : 'pill--short'}`}>
                        {row.direction === 'LONG' ? 'Long' : 'Short'}
                      </span>
                    </td>
                    <td className="num">{formatPrice(row.startPrice) || '—'}</td>
                    <td className="num">{formatPrice(row.livePrice) || '—'}</td>
                    <td className="num">{row.livePrice == null ? '—' : `${row.returnPct >= 0 ? '+' : ''}${row.returnPct.toFixed(2)}%`}</td>
                    <td className={`num ${row.points >= 0 ? 'pos' : 'neg'}`}>{fmtPts(row.points)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {summary.myTeam && (
        <section className="panel">
          <h2>Your roster</h2>
          <div className="table-wrap">
            <table className="roster">
              <thead>
                <tr>
                  <th>Spot</th>
                  <th>Ticker</th>
                  <th>Company</th>
                  <th className="num">Latest close</th>
                </tr>
              </thead>
              <tbody>
                {summary.myTeam.roster.map((row) => (
                  <tr key={row.ticker}>
                    <td>{row.label}</td>
                    <td className="mono strong">{row.ticker}</td>
                    <td>{row.name}</td>
                    <td className="num">{formatPrice(row.price) || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {summary.myTeam.roster.every((row) => row.price == null) && (
            <p className="muted small">Closing prices appear once the price feed has run.</p>
          )}
        </section>
      )}

      <section className="panel">
        <h2>Teams in the league</h2>
        <ul className="team-chips">
          {summary.teams.map((team) => (
            <li key={team.id}>
              <span className="dot" style={{ background: team.color }} aria-hidden="true" />
              {team.name}
              {team.bot && <span className="badge">bot</span>}
            </li>
          ))}
        </ul>
        <p className="muted small">
          {summary.acceptedCount} of {summary.leagueSize} places taken by players. The rest are bots.
        </p>
      </section>
    </div>
  );
}

function Stat({ label, value, tone }) {
  return (
    <div className="stat">
      <span className="stat__label">{label}</span>
      <span className={`stat__value ${tone ?? ''}`}>{value}</span>
    </div>
  );
}

export default DashboardPage;
