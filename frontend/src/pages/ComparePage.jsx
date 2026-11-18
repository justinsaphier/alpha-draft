import { useState } from 'react';
import useLeague from '../hooks/useLeague.js';
import { fmtPts } from '../utils/points.js';

// Head-to-head: two teams side by side, month by month and in total.
function ComparePage({ token, onUnauthorized }) {
  const { summary, standings, error, isLoading } = useLeague(token, onUnauthorized);
  const [leftId, setLeftId] = useState(null);
  const [rightId, setRightId] = useState(null);

  if (isLoading && !summary) {
    return <p className="loading">Loading teams…</p>;
  }
  if (error && !summary) {
    return <p role="alert" className="notice notice--error">Could not load teams: {error}</p>;
  }
  if (!summary) {
    return null;
  }

  const left = standings.find((r) => r.teamId === (leftId ?? standings[0]?.teamId));
  const right = standings.find((r) => r.teamId === (rightId ?? standings[1]?.teamId));

  return (
    <div className="compare">
      <section className="panel">
        <h2>Compare two teams</h2>
        <div className="compare__pickers">
          <TeamSelect label="Team A" standings={standings} value={left?.teamId} onChange={setLeftId} />
          <TeamSelect label="Team B" standings={standings} value={right?.teamId} onChange={setRightId} />
        </div>
        {left && right && (
          <div className="table-wrap">
            <table className="standings">
              <thead>
                <tr>
                  <th>Month</th>
                  <th className="num">{left.name}</th>
                  <th className="num">{right.name}</th>
                </tr>
              </thead>
              <tbody>
                {summary.monthLabels.slice(0, left.monthly.length).map((label, i) => (
                  <tr key={label}>
                    <td>{label}</td>
                    <td className="num">{fmtPts(left.monthly[i])}</td>
                    <td className="num">{fmtPts(right.monthly[i])}</td>
                  </tr>
                ))}
                <tr>
                  <td className="strong">Season</td>
                  <td className="num strong">{fmtPts(left.total)}</td>
                  <td className="num strong">{fmtPts(right.total)}</td>
                </tr>
              </tbody>
            </table>
          </div>
        )}
        {standings.length < 2 && <p className="muted">Two teams are needed to compare.</p>}
      </section>
    </div>
  );
}

function TeamSelect({ label, standings, value, onChange }) {
  return (
    <label className="field">
      <span>{label}</span>
      <select value={value ?? ''} onChange={(e) => onChange(Number(e.target.value))}>
        {standings.map((row) => (
          <option key={row.teamId} value={row.teamId}>
            {row.name}
          </option>
        ))}
      </select>
    </label>
  );
}

export default ComparePage;
