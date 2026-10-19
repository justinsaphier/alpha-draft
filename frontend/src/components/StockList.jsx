import { useState } from 'react';
import StockCard from './StockCard.jsx';
import StockForm from './StockForm.jsx';
import {
  DEFAULT_FILTER,
  DEFAULT_SORT,
  FILTER_STRATEGIES,
  SORT_STRATEGIES,
  applyStrategies,
} from '../strategies/stockListStrategies.js';

function StockList({ stocks, onUpdate, onDelete }) {
  const [editingId, setEditingId] = useState(null);
  const [sortKey, setSortKey] = useState(DEFAULT_SORT);
  const [filterKey, setFilterKey] = useState(DEFAULT_FILTER);

  if (stocks.length === 0) {
    return <p>No stocks available.</p>;
  }

  async function handleSave(id, changes) {
    await onUpdate(id, changes);
    setEditingId(null);
  }

  // StockList only picks which strategies to use; the strategies decide how.
  const visibleStocks = applyStrategies(stocks, FILTER_STRATEGIES[filterKey], SORT_STRATEGIES[sortKey]);

  return (
    <section className="stock-list-section">
      <div className="stock-list-controls">
        <label htmlFor="stock-sort">Sort by</label>
        <select id="stock-sort" value={sortKey} onChange={(e) => setSortKey(e.target.value)}>
          {Object.entries(SORT_STRATEGIES).map(([key, strategy]) => (
            <option key={key} value={key}>
              {strategy.label}
            </option>
          ))}
        </select>

        <label htmlFor="stock-filter">Filter by sector</label>
        <select id="stock-filter" value={filterKey} onChange={(e) => setFilterKey(e.target.value)}>
          {Object.entries(FILTER_STRATEGIES).map(([key, strategy]) => (
            <option key={key} value={key}>
              {strategy.label}
            </option>
          ))}
        </select>
      </div>

      {visibleStocks.length === 0 ? (
        <p>No stocks match this filter.</p>
      ) : (
        <ul className="stock-list">
          {visibleStocks.map((stock) =>
            stock.id === editingId ? (
              <li key={stock.id} className="stock-card stock-card--editing">
                <StockForm
                  initialStock={stock}
                  submitLabel="Save"
                  onSubmit={(changes) => handleSave(stock.id, changes)}
                  onCancel={() => setEditingId(null)}
                />
              </li>
            ) : (
              <StockCard
                key={stock.id}
                stock={stock}
                onEdit={onUpdate && (() => setEditingId(stock.id))}
                onDelete={onDelete && (() => onDelete(stock.id))}
              />
            ),
          )}
        </ul>
      )}
    </section>
  );
}

export default StockList;