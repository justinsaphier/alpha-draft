import { useState } from 'react';
import { SECTORS } from '../constants/sectors.js';

const EMPTY_STOCK = { ticker: '', name: '', sector: SECTORS[0] };

// Used both to create a new stock and to edit an existing one in the list.
function StockForm({ initialStock = EMPTY_STOCK, submitLabel = 'Add Stock', onSubmit, onCancel }) {
  const [ticker, setTicker] = useState(initialStock.ticker);
  const [name, setName] = useState(initialStock.name);
  const [sector, setSector] = useState(initialStock.sector);

  async function handleSubmit(e) {
    e.preventDefault();
    try {
      await onSubmit({ ticker: ticker.trim().toUpperCase(), name: name.trim(), sector });
    } catch {
      // The page shows the error; keep what the user typed so they can retry.
      return;
    }

    // Clear the create form after a successful add; edit mode is closed by the parent.
    if (!onCancel) {
      setTicker('');
      setName('');
      setSector(SECTORS[0]);
    }
  }

  return (
    <form className="stock-form" onSubmit={handleSubmit}>
      <input
        aria-label="Ticker"
        placeholder="Ticker"
        value={ticker}
        onChange={(e) => setTicker(e.target.value)}
        required
        maxLength={10}
      />
      <input
        aria-label="Company name"
        placeholder="Company name"
        value={name}
        onChange={(e) => setName(e.target.value)}
        required
      />
      <select aria-label="Sector" value={sector} onChange={(e) => setSector(e.target.value)}>
        {SECTORS.map((s) => (
          <option key={s} value={s}>
            {s}
          </option>
        ))}
      </select>
      <button type="submit">{submitLabel}</button>
      {onCancel && (
        <button type="button" onClick={onCancel}>
          Cancel
        </button>
      )}
    </form>
  );
}

export default StockForm;