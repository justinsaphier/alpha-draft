import { fmtPts } from '../utils/points.js';

// The league table: rank, team, one column per finished month, the live month and the season total.
// Clicking a row selects that team (when onSelect is given).
function StandingsTable({ standings, monthLabels, selectedId, onSelect }) {
  if (standings.length === 0) {
    return <p className="empty">No teams yet.</p>;
  }
  return (
    <div className="table-wrap">
      <table className="standings">
        <thead>
          <tr>
            <th>#</th>
            <th>Team</th>
            {monthLabels.map((label, i) => (
              <th key={label} className="num">
                {label}
              </th>
            ))}
            <th className="num">Live</th>
            <th className="num">Total</th>
            <th className="num">Best</th>
          </tr>
        </thead>
        <tbody>
          {standings.map((row) => (
            <tr
              key={row.teamId}
              className={[onSelect ? 'is-clickable' : '', row.teamId === selectedId ? 'is-selected' : ''].join(' ')}
              onClick={onSelect ? () => onSelect(row.teamId) : undefined}
            >
              <td>{row.rank}</td>
              <td>
                <span className="dot" style={{ background: row.color }} aria-hidden="true" />
                {row.name}
                {row.bot && <span className="badge">bot</span>}
              </td>
              {row.monthly.map((points, i) => (
                <td key={i} className={`num ${points >= 0 ? 'pos' : 'neg'}`}>
                  {fmtPts(points)}
                </td>
              ))}
              <td className="num">{row.live == null ? '—' : fmtPts(row.live)}</td>
              <td className={`num strong ${row.total >= 0 ? 'pos' : 'neg'}`}>{fmtPts(row.total)}</td>
              <td className="num">{fmtPts(row.best)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default StandingsTable;
