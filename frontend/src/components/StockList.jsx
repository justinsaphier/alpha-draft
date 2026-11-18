import { useState } from 'react';
import StockCard from './StockCard.jsx';
import StockForm from './StockForm.jsx';
import StockListControls from './StockListControls.jsx';
import {
  DEFAULT_FILTER,
  DEFAULT_SORT,
  FILTER_STRATEGIES,
  SORT_STRATEGIES,
  applyStrategies,
  searchStocks,
} from '../strategies/stockListStrategies.js';

// prices: { [stockId]: { closePrice, tradeDate } }, optional.
function StockList({ stocks, prices = {}, owners = {}, onUpdate, onDelete }) {
  const [editingId, setEditingId] = useState(null);
  const [sortKey, setSortKey] = useState(DEFAULT_SORT);
  const [filterKey, setFilterKey] = useState(DEFAULT_FILTER);
  const [query, setQuery] = useState('');

  if (stocks.length === 0) {
    return <p className="empty">No stocks in the pool yet.</p>;
  }

  async function handleSave(id, changes) {
    await onUpdate(id, changes);
    setEditingId(null);
  }

  // StockList only picks which strategies to use; the strategies decide how.
  const visibleStocks = applyStrategies(
    searchStocks(stocks, query),
    FILTER_STRATEGIES[filterKey],
    SORT_STRATEGIES[sortKey],
  );

  return (
    <section className="stock-list-section" aria-label="Stock pool">
      <StockListControls
        query={query}
        sortKey={sortKey}
        filterKey={filterKey}
        onQueryChange={setQuery}
        onSortChange={setSortKey}
        onFilterChange={setFilterKey}
      />

      <p className="result-count" aria-live="polite">
        Showing {visibleStocks.length} of {stocks.length} stocks
      </p>

      {visibleStocks.length === 0 ? (
        <p className="empty">No stocks match your search.</p>
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
                price={prices[stock.id]}
                owner={owners[stock.ticker]}
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
