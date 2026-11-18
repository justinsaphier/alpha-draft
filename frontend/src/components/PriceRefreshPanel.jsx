import { formatDateTime } from '../utils/format.js';

// Admin-only card for starting the closing-price update and seeing how the last one went.
function PriceRefreshPanel({ status, error, onRefresh }) {
  const running = Boolean(status?.running);
  const configured = status ? status.configured : true;
  const finished = status?.lastFinishedAt ? new Date(status.lastFinishedAt) : null;

  let summary = 'No closing prices stored yet.';
  if (running) {
    summary = 'Updating closing prices. The full index takes about ten minutes; this page updates by itself.';
  } else if (finished) {
    summary = `Last update finished ${formatDateTime(finished)}, storing ${status.lastStored ?? 0} new close${
      status.lastStored === 1 ? '' : 's'
    }.`;
  }

  return (
    <section className="panel" aria-labelledby="price-panel-title">
      <h2 id="price-panel-title">Closing prices</h2>
      <p className="panel__text">{summary}</p>
      {!configured && (
        <p className="notice" role="status">
          Price updates are off until the server has a TWELVE_DATA_API_KEY.
        </p>
      )}
      {status?.lastError && !running && (
        <p className="notice notice--error" role="alert">
          The last update failed: {status.lastError}
        </p>
      )}
      {error && (
        <p className="notice notice--error" role="alert">
          {error}
        </p>
      )}
      <button
        type="button"
        className="button button--primary"
        onClick={onRefresh}
        disabled={running || !configured}
      >
        {running ? 'Updating…' : 'Refresh closing prices'}
      </button>
    </section>
  );
}

export default PriceRefreshPanel;
