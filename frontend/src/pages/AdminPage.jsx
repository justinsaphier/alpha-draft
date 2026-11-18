import { useEffect, useState } from 'react';
import PriceRefreshPanel from '../components/PriceRefreshPanel.jsx';
import usePrices from '../hooks/usePrices.js';
import {
  acceptTeam,
  clearClosePrice,
  finalizeMonth,
  getAdminLeague,
  rejectTeam,
  setClosePrice,
  startDraftNow,
  startSeason,
  updateLeagueSettings,
} from '../services/leagueService.js';
import { formatDateTime } from '../utils/format.js';

// Turns a stored UTC instant into the value a datetime-local input expects (local time).
function toLocalInput(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// The month after today, as yyyy-mm, used as the default first month of the season.
function nextMonthInput() {
  const d = new Date();
  const next = new Date(d.getFullYear(), d.getMonth() + 1, 1);
  return `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}`;
}

// Everything the admin decides: who joins, when the draft runs, when months close, and prices.
function AdminPage({ token, onUnauthorized }) {
  const [view, setView] = useState(null);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [busy, setBusy] = useState(false);
  const [settings, setSettings] = useState(null);
  const [override, setOverride] = useState({ ticker: '', month: -1, close: '' });
  const { status: priceStatus, refreshError, startRefresh } = usePrices(token, true, onUnauthorized);

  async function load() {
    try {
      const next = await getAdminLeague(token);
      setView(next);
      setSettings({
        name: next.name,
        leagueSize: next.leagueSize,
        draftAt: toLocalInput(next.draftAt),
        firstMonth: next.firstMonth ?? nextMonthInput(),
      });
      setError(null);
    } catch (err) {
      if (err.status === 401) onUnauthorized?.();
      setError(err.message);
    }
  }

  useEffect(() => {
    load();
    // Reload only when the token changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  async function act(action, success) {
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await action();
      setMessage(success);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  if (!view || !settings) {
    return error ? <p role="alert" className="notice notice--error">{error}</p> : <p className="loading">Loading the admin console…</p>;
  }

  const locked = view.status !== 'REGISTRATION';
  const tickers = view.prices.map((p) => p.ticker);

  function saveSettings(event) {
    event.preventDefault();
    act(
      () =>
        updateLeagueSettings(
          {
            name: settings.name,
            leagueSize: Number(settings.leagueSize),
            draftAt: settings.draftAt ? new Date(settings.draftAt).toISOString() : null,
            firstMonth: settings.firstMonth,
          },
          token,
        ),
      'Settings saved.',
    );
  }

  return (
    <div className="admin">
      <header className="page-header">
        <h1>Admin console</h1>
        <p>Status: <strong>{view.status.replace('_', ' ').toLowerCase()}</strong>
          {view.status === 'IN_SEASON' && ` · ${view.monthLabels[view.currentMonth]} is live`}
        </p>
      </header>

      {message && <p className="notice" role="status">{message}</p>}
      {error && <p className="notice notice--error" role="alert">{error}</p>}

      <div className="admin-grid">
        <section className="panel" aria-labelledby="settings-title">
          <h2 id="settings-title">League settings</h2>
          <form className="stack" onSubmit={saveSettings}>
            <label className="field">
              <span>League name</span>
              <input value={settings.name} disabled={locked} onChange={(e) => setSettings({ ...settings, name: e.target.value })} />
            </label>
            <label className="field">
              <span>Teams in the league (6–10)</span>
              <select value={settings.leagueSize} disabled={locked} onChange={(e) => setSettings({ ...settings, leagueSize: e.target.value })}>
                {[6, 7, 8, 9, 10].map((n) => (
                  <option key={n} value={n}>{n}</option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Draft day and time (blank = start by hand)</span>
              <input type="datetime-local" value={settings.draftAt} disabled={locked} onChange={(e) => setSettings({ ...settings, draftAt: e.target.value })} />
            </label>
            <label className="field">
              <span>First month of the quarter (yyyy-mm)</span>
              <input placeholder="2026-12" value={settings.firstMonth} disabled={locked} onChange={(e) => setSettings({ ...settings, firstMonth: e.target.value })} />
            </label>
            {!locked && (
              <button type="submit" className="button button--primary" disabled={busy}>Save settings</button>
            )}
            {locked && <p className="muted small">Settings are fixed once the draft starts.</p>}
          </form>
        </section>

        <section className="panel" aria-labelledby="season-title">
          <h2 id="season-title">Draft and season</h2>
          <p className="muted small">
            {view.status === 'REGISTRATION' && (view.draftAt
              ? `The draft starts automatically ${formatDateTime(view.draftAt)}.`
              : 'No draft day set. Start it by hand when everyone is in.')}
            {view.status === 'DRAFTING' && 'Bots pick on their turns. Players pick when it is their turn.'}
            {view.status === 'PRESEASON' && 'Draft complete. Start the season when the first month’s calls are in.'}
            {view.status === 'IN_SEASON' && 'Finalize a month when every drafted stock has a closing price.'}
            {view.status === 'COMPLETE' && 'All three months are scored.'}
          </p>
          <div className="actions">
            {view.status === 'REGISTRATION' && (
              <button type="button" className="button button--primary" disabled={busy} onClick={() => act(() => startDraftNow(token), 'Draft started.')}>
                Start draft now
              </button>
            )}
            {view.status === 'PRESEASON' && (
              <button type="button" className="button button--primary" disabled={busy} onClick={() => act(() => startSeason(token), 'Season started.')}>
                Start season
              </button>
            )}
            {view.status === 'IN_SEASON' && (
              <button type="button" className="button button--primary" disabled={busy} onClick={() => act(() => finalizeMonth(token), 'Month finalized and scored.')}>
                Finalize {view.monthLabels[view.currentMonth]}
              </button>
            )}
          </div>
          {view.status === 'IN_SEASON' && view.missingPrices.length > 0 && (
            <p className="notice notice--error">
              No closing price yet for: {view.missingPrices.join(', ')}. Refresh prices or enter them below.
            </p>
          )}
        </section>

        <section className="panel" aria-labelledby="requests-title">
          <h2 id="requests-title">Join requests</h2>
          {view.pending.length === 0 && <p className="muted">No one is waiting.</p>}
          <ul className="list">
            {view.pending.map((p) => (
              <li key={p.id}>
                <div className="grow">
                  <strong>{p.name}</strong>
                  <span className="muted small"> {p.email}</span>
                </div>
                <button type="button" className="button button--primary" disabled={busy} onClick={() => act(() => acceptTeam(p.id, token), `${p.name} accepted.`)}>Accept</button>
                <button type="button" className="button" disabled={busy} onClick={() => act(() => rejectTeam(p.id, token), `${p.name} turned down.`)}>Decline</button>
              </li>
            ))}
          </ul>
          <h3>Accepted players</h3>
          <ul className="list">
            {view.accepted.map((p) => (
              <li key={p.id}>
                <span className="grow">{p.name}</span>
                <span className="muted small">{p.email}</span>
              </li>
            ))}
            {view.accepted.length === 0 && <li className="muted">None yet.</li>}
          </ul>
          <p className="muted small">
            {view.accepted.length} {view.accepted.length === 1 ? 'player' : 'players'} accepted. {view.botCount} bot{' '}
            {view.botCount === 1 ? 'team fills' : 'teams fill'} the rest of the league.
          </p>
        </section>

        <div className="stack">
          <PriceRefreshPanel status={priceStatus} error={refreshError} onRefresh={startRefresh} />

          <section className="panel" aria-labelledby="price-title">
            <h2 id="price-title">Enter a closing price by hand</h2>
            <p className="muted small">Corrections replace the feed’s price for that month. Month -1 is the starting price.</p>
            <form
              className="inline-form"
              onSubmit={(e) => {
                e.preventDefault();
                act(() => setClosePrice(override.ticker, override.month, Number(override.close), token), `${override.ticker} price saved.`);
              }}
            >
              <select aria-label="Ticker" value={override.ticker} onChange={(e) => setOverride({ ...override, ticker: e.target.value })}>
                <option value="">Ticker…</option>
                {tickers.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
              <select aria-label="Month" value={override.month} onChange={(e) => setOverride({ ...override, month: Number(e.target.value) })}>
                <option value={-1}>Start</option>
                {view.monthLabels.map((label, i) => <option key={label} value={i}>{label}</option>)}
              </select>
              <input aria-label="Close price" type="number" min="0" step="0.01" placeholder="Price" value={override.close} onChange={(e) => setOverride({ ...override, close: e.target.value })} />
              <button type="submit" className="button" disabled={busy || !override.ticker || !override.close}>Save price</button>
            </form>
          </section>
        </div>
      </div>

      <section className="panel">
        <h2>Drafted stocks and their prices</h2>
        <div className="table-wrap">
          <table className="standings">
            <thead>
              <tr>
                <th>Ticker</th>
                <th>Company</th>
                <th className="num">Latest</th>
                <th className="num">Start</th>
                {view.monthLabels.map((label) => <th key={label} className="num">{label}</th>)}
              </tr>
            </thead>
            <tbody>
              {view.prices.map((row) => (
                <tr key={row.ticker}>
                  <td className="mono strong">{row.ticker}</td>
                  <td>{row.name}</td>
                  <td className="num">{row.livePrice == null ? '—' : `$${Number(row.livePrice).toFixed(2)}`}</td>
                  {[-1, 0, 1, 2].map((m) => (
                    <PriceCell key={m} row={row} month={m} busy={busy} token={token} onAct={act} />
                  ))}
                </tr>
              ))}
              {view.prices.length === 0 && <tr><td colSpan={8} className="empty">Stocks appear here once the draft is done.</td></tr>}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <h2>Audit log</h2>
        <ul className="audit">
          {view.audit.map((a, i) => (
            <li key={i}>
              <span className="muted small">{formatDateTime(a.at)}</span>
              <span className="badge">{a.action}</span>
              <span>{a.detail}</span>
            </li>
          ))}
          {view.audit.length === 0 && <li className="muted">Nothing recorded yet.</li>}
        </ul>
      </section>
    </div>
  );
}

function PriceCell({ row, month, busy, token, onAct }) {
  const cell = row.months?.[month];
  if (!cell) {
    return <td className="num muted">—</td>;
  }
  return (
    <td className={`num ${cell.manual ? 'is-manual' : ''}`}>
      ${Number(cell.price).toFixed(2)}
      {cell.manual && (
        <button type="button" className="link" disabled={busy} onClick={() => onAct(() => clearClosePrice(row.ticker, month, token), `${row.ticker} reverted to the feed.`)}>
          reset
        </button>
      )}
    </td>
  );
}

export default AdminPage;
