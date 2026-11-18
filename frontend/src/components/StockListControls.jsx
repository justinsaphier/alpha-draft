import { FILTER_STRATEGIES, SORT_STRATEGIES } from '../strategies/stockListStrategies.js';

// One labelled dropdown built from a strategy map ({ key: { label } }).
function StrategySelect({ id, label, strategies, value, onChange }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <select id={id} value={value} onChange={(e) => onChange(e.target.value)}>
        {Object.entries(strategies).map(([key, strategy]) => (
          <option key={key} value={key}>
            {strategy.label}
          </option>
        ))}
      </select>
    </div>
  );
}

// Search box plus the sort and filter dropdowns above the stock list. StockList owns the
// chosen values; this component only renders them and reports changes.
function StockListControls({ query, sortKey, filterKey, onQueryChange, onSortChange, onFilterChange }) {
  return (
    <div className="stock-list-controls">
      <div className="field field--search">
        <label htmlFor="stock-search">Search</label>
        <input
          id="stock-search"
          type="search"
          placeholder="Ticker or company"
          value={query}
          onChange={(e) => onQueryChange(e.target.value)}
        />
      </div>
      <StrategySelect
        id="stock-filter"
        label="Sector"
        strategies={FILTER_STRATEGIES}
        value={filterKey}
        onChange={onFilterChange}
      />
      <StrategySelect
        id="stock-sort"
        label="Sort by"
        strategies={SORT_STRATEGIES}
        value={sortKey}
        onChange={onSortChange}
      />
    </div>
  );
}

export default StockListControls;
