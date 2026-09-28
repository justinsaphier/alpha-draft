import { render, screen, fireEvent, waitForElementToBeRemoved } from '@testing-library/react';
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


test('edits a stock through PUT and shows the change without reloading', async () => {
    // Arrange
    stockService.updateStock.mockResolvedValue({ ...APPLE, name: 'Apple Incorporated' });
    render(<StocksPage />);
    await screen.findByText('AAPL');
    fireEvent.click(screen.getByText('Edit'));
    fireEvent.change(screen.getAllByLabelText('Company name')[1], {
      target: { value: 'Apple Incorporated' },
    });
  
    // Act
    fireEvent.click(screen.getByText('Save'));
  
    // Assert
    expect(await screen.findByText('Apple Incorporated')).toBeInTheDocument();
    expect(stockService.updateStock).toHaveBeenCalledWith(1, {
      ticker: 'AAPL',
      name: 'Apple Incorporated',
      sector: 'Technology',
    });
    expect(screen.queryByText('Save')).not.toBeInTheDocument();
    expect(stockService.getStocks).toHaveBeenCalledTimes(1);
  });
  
  test('cancelling an edit leaves the stock unchanged', async () => {
    // Arrange
    render(<StocksPage />);
    await screen.findByText('AAPL');
    fireEvent.click(screen.getByText('Edit'));
  
    // Act
    fireEvent.click(screen.getByText('Cancel'));
  
    // Assert
    expect(screen.getByText('Apple Inc.')).toBeInTheDocument();
    expect(stockService.updateStock).not.toHaveBeenCalled();
  });
  
  test('deletes a stock through DELETE and removes it without reloading', async () => {
    // Arrange
    stockService.deleteStock.mockResolvedValue(undefined);
    render(<StocksPage />);
    await screen.findByText('AAPL');
  
    // Act
    fireEvent.click(screen.getByText('Delete'));
  
    // Assert
    await waitForElementToBeRemoved(() => screen.queryByText('AAPL'));
    expect(stockService.deleteStock).toHaveBeenCalledWith(1);
    expect(stockService.getStocks).toHaveBeenCalledTimes(1);
  });
  
  test('keeps the stock and shows an error when delete fails', async () => {
    // Arrange
    stockService.deleteStock.mockRejectedValue(new Error('Request failed: 404'));
    render(<StocksPage />);
    await screen.findByText('AAPL');
  
    // Act
    fireEvent.click(screen.getByText('Delete'));
  
    // Assert
    expect(await screen.findByText(/Could not delete stock/)).toBeInTheDocument();
    expect(screen.getByText('AAPL')).toBeInTheDocument();
  });