import { describe, expect, it } from 'vitest';
import { SECTORS } from '../constants/sectors.js';
import { FILTER_STRATEGIES, SORT_STRATEGIES, applyStrategies } from './stockListStrategies.js';

const [sectorA, sectorB] = SECTORS;

const stocks = [
  { id: 1, ticker: 'MSFT', name: 'Microsoft', sector: sectorB },
  { id: 2, ticker: 'AAPL', name: 'Zeta Apple Co', sector: sectorA },
  { id: 3, ticker: 'NVDA', name: 'Nvidia', sector: sectorA },
];

const tickers = (list) => list.map((s) => s.ticker);

describe('stock list strategies', () => {
  it('keeps the original order with the default sort', () => {
    const result = applyStrategies(stocks, FILTER_STRATEGIES.all, SORT_STRATEGIES.added);
    expect(tickers(result)).toEqual(['MSFT', 'AAPL', 'NVDA']);
  });

  it('sorts by ticker in both directions', () => {
    expect(tickers(applyStrategies(stocks, FILTER_STRATEGIES.all, SORT_STRATEGIES.tickerAsc))).toEqual([
      'AAPL',
      'MSFT',
      'NVDA',
    ]);
    expect(tickers(applyStrategies(stocks, FILTER_STRATEGIES.all, SORT_STRATEGIES.tickerDesc))).toEqual([
      'NVDA',
      'MSFT',
      'AAPL',
    ]);
  });

  it('sorts by company name', () => {
    const result = applyStrategies(stocks, FILTER_STRATEGIES.all, SORT_STRATEGIES.name);
    expect(tickers(result)).toEqual(['MSFT', 'NVDA', 'AAPL']);
  });

  it('filters to a single sector', () => {
    const result = applyStrategies(stocks, FILTER_STRATEGIES[sectorA], SORT_STRATEGIES.added);
    expect(tickers(result)).toEqual(['AAPL', 'NVDA']);
  });

  it('does not change the original list', () => {
    applyStrategies(stocks, FILTER_STRATEGIES.all, SORT_STRATEGIES.tickerAsc);
    expect(tickers(stocks)).toEqual(['MSFT', 'AAPL', 'NVDA']);
  });
});