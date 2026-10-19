import { SECTORS } from '../constants/sectors.js';

// Strategy pattern: every sort strategy has the same shape ({ label, compare })
// and every filter strategy has the same shape ({ label, matches }), so
// StockList can swap them at runtime without knowing how any of them work.
// To add a new sort or filter, add an entry here; StockList does not change.

export const SORT_STRATEGIES = {
  added: { label: 'Date added', compare: () => 0 },
  tickerAsc: { label: 'Ticker (A–Z)', compare: (a, b) => a.ticker.localeCompare(b.ticker) },
  tickerDesc: { label: 'Ticker (Z–A)', compare: (a, b) => b.ticker.localeCompare(a.ticker) },
  name: { label: 'Company name (A–Z)', compare: (a, b) => a.name.localeCompare(b.name) },
  sector: {
    label: 'Sector',
    compare: (a, b) => a.sector.localeCompare(b.sector) || a.ticker.localeCompare(b.ticker),
  },
};

function sectorFilter(sector) {
  return { label: sector, matches: (stock) => stock.sector === sector };
}

export const FILTER_STRATEGIES = {
  all: { label: 'All sectors', matches: () => true },
  ...Object.fromEntries(SECTORS.map((sector) => [sector, sectorFilter(sector)])),
};

export const DEFAULT_SORT = 'added';
export const DEFAULT_FILTER = 'all';

// Applies one filter strategy and one sort strategy without changing the input list.
export function applyStrategies(stocks, filterStrategy, sortStrategy) {
  return stocks.filter(filterStrategy.matches).sort(sortStrategy.compare);
}