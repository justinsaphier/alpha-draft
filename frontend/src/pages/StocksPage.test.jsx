import { render, screen, fireEvent, waitForElementToBeRemoved } from '@testing-library/react';
import { vi } from 'vitest';
import StocksPage from './StocksPage';
import { AuthProvider } from '../auth/AuthContext.jsx';
import * as stockService from '../services/stockService.js';

vi.mock('../services/stockService.js');

const APPLE = { id: 1, ticker: 'AAPL', name: 'Apple Inc.', sector: 'Technology' };

const ADMIN_SESSION = { token: 'admin-token', email: 'admin@example.com', role: 'ADMIN' };
const PLAYER_SESSION = { token: 'player-token', email: 'player@example.com', role: 'PLAYER' };

// AuthProvider reads the saved session on start, so saving one first renders the page
// as that user. No session means signed out.
function renderPage(session = ADMIN_SESSION) {
  if (session) {
    window.localStorage.setItem('alphaDraft.session', JSON.stringify(session));
  }
  return render(
    <AuthProvider>
      <StocksPage />
    </AuthProvider>,
  );
}

afterEach(() => {
  window.localStorage.clear();
});

beforeEach(() => {
  vi.resetAllMocks();
  stockService.getStocks.mockResolvedValue([APPLE]);
});

test('shows the stocks loaded from the backend', async () => {
  renderPage();
  expect(await screen.findByText('AAPL')).toBeInTheDocument();
});

test('adds a created stock to the list without reloading', async () => {
  stockService.createStock.mockResolvedValue({
    id: 7,
    ticker: 'MSFT',
    name: 'Microsoft Corporation',
    sector: 'Technology',
  });
  renderPage();
  await screen.findByText('AAPL');
  fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'MSFT' } });
  fireEvent.change(screen.getByLabelText('Company name'), {
    target: { value: 'Microsoft Corporation' },
  });

  fireEvent.click(screen.getByText('Add Stock'));

  expect(await screen.findByText('MSFT')).toBeInTheDocument();
  expect(screen.getByText('AAPL')).toBeInTheDocument();
  expect(stockService.getStocks).toHaveBeenCalledTimes(1);
});

test('shows an error when the create request fails', async () => {
  stockService.createStock.mockRejectedValue(new Error('Request failed: 400'));
  renderPage();
  await screen.findByText('AAPL');
  fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'BAD' } });
  fireEvent.change(screen.getByLabelText('Company name'), { target: { value: 'Bad Co' } });

  fireEvent.click(screen.getByText('Add Stock'));

  expect(await screen.findByText(/Could not add BAD/)).toBeInTheDocument();
});

test('edits a stock through PUT and shows the change without reloading', async () => {
  stockService.updateStock.mockResolvedValue({ ...APPLE, name: 'Apple Incorporated' });
  renderPage();
  await screen.findByText('AAPL');
  fireEvent.click(screen.getByText('Edit'));
  fireEvent.change(screen.getAllByLabelText('Company name')[1], {
    target: { value: 'Apple Incorporated' },
  });

  fireEvent.click(screen.getByText('Save'));

  expect(await screen.findByText('Apple Incorporated')).toBeInTheDocument();
  expect(stockService.updateStock).toHaveBeenCalledWith(1, {
    ticker: 'AAPL',
    name: 'Apple Incorporated',
    sector: 'Technology',
  }, 'admin-token');
  expect(screen.queryByText('Save')).not.toBeInTheDocument();
  expect(stockService.getStocks).toHaveBeenCalledTimes(1);
});

test('cancelling an edit leaves the stock unchanged', async () => {
  renderPage();
  await screen.findByText('AAPL');
  fireEvent.click(screen.getByText('Edit'));

  fireEvent.click(screen.getByText('Cancel'));

  expect(screen.getByText('Apple Inc.')).toBeInTheDocument();
  expect(stockService.updateStock).not.toHaveBeenCalled();
});

test('deletes a stock through DELETE and removes it without reloading', async () => {
  stockService.deleteStock.mockResolvedValue(undefined);
  renderPage();
  await screen.findByText('AAPL');

  fireEvent.click(screen.getByText('Delete'));

  await waitForElementToBeRemoved(() => screen.queryByText('AAPL'));
  expect(stockService.deleteStock).toHaveBeenCalledWith(1, 'admin-token');
  expect(stockService.getStocks).toHaveBeenCalledTimes(1);
});

test('keeps the stock and shows an error when delete fails', async () => {
  stockService.deleteStock.mockRejectedValue(new Error('Request failed: 404'));
  renderPage();
  await screen.findByText('AAPL');

  fireEvent.click(screen.getByText('Delete'));

  expect(await screen.findByText(/Could not delete stock/)).toBeInTheDocument();
  expect(screen.getByText('AAPL')).toBeInTheDocument();
});

describe('admin-only controls', () => {
  test('an admin sees the add form and Edit/Delete buttons', async () => {
    renderPage(ADMIN_SESSION);

    await screen.findByText('AAPL');

    expect(screen.getByText('Add a stock')).toBeInTheDocument();
    expect(screen.getByLabelText('Edit AAPL')).toBeInTheDocument();
    expect(screen.getByLabelText('Delete AAPL')).toBeInTheDocument();
  });

  test('a player sees the stocks but no add form or Edit/Delete buttons', async () => {
    renderPage(PLAYER_SESSION);

    await screen.findByText('AAPL');

    expect(screen.queryByText('Add a stock')).not.toBeInTheDocument();
    expect(screen.queryByText('Add Stock')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Edit AAPL')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Delete AAPL')).not.toBeInTheDocument();
  });

  test('loads stocks with the signed-in user\'s token', async () => {
    renderPage(PLAYER_SESSION);

    await screen.findByText('AAPL');

    expect(stockService.getStocks).toHaveBeenCalledWith('player-token');
  });

  test('signed out shows a sign-in prompt and loads nothing', () => {
    renderPage(null);

    expect(screen.getByText('Sign in to see the stock pool.')).toBeInTheDocument();
    expect(stockService.getStocks).not.toHaveBeenCalled();
  });
});

test('reloads the stock pool with the new token when a different user signs in', async () => {
  const { unmount } = renderPage(ADMIN_SESSION);
  await screen.findByText('AAPL');
  unmount();
  window.localStorage.clear();

  renderPage(PLAYER_SESSION);
  await screen.findByText('AAPL');

  expect(stockService.getStocks).toHaveBeenLastCalledWith('player-token');
  expect(screen.queryByLabelText('Edit AAPL')).not.toBeInTheDocument();
});

test('an expired session is signed out instead of showing a failed load', async () => {
  stockService.getStocks.mockRejectedValue(
    Object.assign(new Error('Request failed: 401'), { status: 401 }),
  );

  renderPage(PLAYER_SESSION);

  expect(await screen.findByText('Sign in to see the stock pool.')).toBeInTheDocument();
  expect(window.localStorage.getItem('alphaDraft.session')).toBeNull();
});
