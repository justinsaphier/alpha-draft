import { render, screen, fireEvent } from '@testing-library/react';
import { vi } from 'vitest';
import StocksPage from './StocksPage';
import * as stockService from '../services/stockService.js';

vi.mock('../services/stockService.js');

const APPLE = { id: 1, ticker: 'AAPL', name: 'Apple Inc.', sector: 'Technology' };

beforeEach(() => {
  vi.resetAllMocks();
  stockService.getStocks.mockResolvedValue([APPLE]);
});

test('shows the stocks loaded from the backend', async () => {
  // Arrange
  render(<StocksPage />);

  // Act — nothing to act on, the page loads on mount

  // Assert
  expect(await screen.findByText('AAPL')).toBeInTheDocument();
});

test('adds a created stock to the list without reloading', async () => {
  // Arrange
  stockService.createStock.mockResolvedValue({
    id: 7,
    ticker: 'MSFT',
    name: 'Microsoft Corporation',
    sector: 'Technology',
  });
  render(<StocksPage />);
  await screen.findByText('AAPL');
  fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'MSFT' } });
  fireEvent.change(screen.getByLabelText('Company name'), {
    target: { value: 'Microsoft Corporation' },
  });

  // Act
  fireEvent.click(screen.getByText('Add Stock'));

  // Assert
  expect(await screen.findByText('MSFT')).toBeInTheDocument();
  expect(screen.getByText('AAPL')).toBeInTheDocument();
  expect(stockService.getStocks).toHaveBeenCalledTimes(1);
});

test('shows an error when the create request fails', async () => {
  // Arrange
  stockService.createStock.mockRejectedValue(new Error('Request failed: 400'));
  render(<StocksPage />);
  await screen.findByText('AAPL');
  fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'BAD' } });
  fireEvent.change(screen.getByLabelText('Company name'), { target: { value: 'Bad Co' } });

  // Act
  fireEvent.click(screen.getByText('Add Stock'));

  // Assert
  expect(await screen.findByText(/Could not add BAD/)).toBeInTheDocument();
});