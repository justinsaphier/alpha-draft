import { FILTER_STRATEGIES, SORT_STRATEGIES } from '../strategies/stockListStrategies.js';

// One labelled dropdown built from a strategy map ({ key: { label } }).
function StrategySelect({ id, label, strategies, value, onChange }) {
  return (
    <>
      <label htmlFor={id}>{label}</label>
      <select id={id} value={value} onChange={(e) => onChange(e.target.value)}>
        {Object.entries(strategies).map(([key, strategy]) => (
          <option key={key} value={key}>
            {strategy.label}
          </option>
        ))}
      </select>
    </>
  );
}

// The sort and filter dropdowns above the stock list. StockList owns the
// chosen keys; this component only renders them and reports changes.
function StockListControls({ sortKey, filterKey, onSortChange, onFilterChange }) {
  return (
    <div className="stock-list-controls">
      <StrategySelect
        id="stock-sort"
        label="Sort by"
        strategies={SORT_STRATEGIES}
        value={sortKey}
        onChange={onSortChange}
      />
      <StrategySelect
        id="stock-filter"
        label="Filter by sector"
        strategies={FILTER_STRATEGIES}
        value={filterKey}
        onChange={onFilterChange}
      />
    </div>
  );
}

export default StockListControls;