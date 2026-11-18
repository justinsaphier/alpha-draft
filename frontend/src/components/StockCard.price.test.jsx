import { render, screen } from '@testing-library/react';
import { expect, test } from 'vitest';
import StockCard from './StockCard.jsx';

const APPLE = { id: 1, ticker: 'AAPL', name: 'Apple Inc.', sector: 'Technology' };

test('shows the latest close with its trade date', () => {
  render(<StockCard stock={APPLE} price={{ closePrice: 230.5, tradeDate: '2026-11-13' }} />);

  expect(screen.getByText('$230.50')).toBeInTheDocument();
  expect(screen.getByText('Close · Nov 13, 2026')).toBeInTheDocument();
});

test('says there is no close yet when the stock has no price', () => {
  render(<StockCard stock={APPLE} />);

  expect(screen.getByText('No close yet')).toBeInTheDocument();
});
