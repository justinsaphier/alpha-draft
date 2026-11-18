import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import usePrices from './usePrices.js';
import * as stockService from '../services/stockService.js';

vi.mock('../services/stockService.js');

const PRICE = { stockId: 1, closePrice: 230.5, tradeDate: '2026-11-13', source: 'twelvedata' };
const IDLE = { configured: true, running: false, lastFinishedAt: null, lastStored: null, lastError: null };

beforeEach(() => {
  vi.resetAllMocks();
  stockService.getLatestPrices.mockResolvedValue([PRICE]);
  stockService.getRefreshStatus.mockResolvedValue(IDLE);
});

afterEach(() => {
  vi.useRealTimers();
});

test('loads prices keyed by stock id for everyone', async () => {
  const { result } = renderHook(() => usePrices('player-token', false, () => {}));

  await waitFor(() => expect(result.current.pricesByStockId[1]).toEqual(PRICE));
  expect(stockService.getRefreshStatus).not.toHaveBeenCalled();
});

test('admins also load the refresh status', async () => {
  const { result } = renderHook(() => usePrices('admin-token', true, () => {}));

  await waitFor(() => expect(result.current.status).toEqual(IDLE));
  expect(stockService.getRefreshStatus).toHaveBeenCalledWith('admin-token');
});

test('starting a refresh shows a plain message when the server has no API key', async () => {
  stockService.startPriceRefresh.mockRejectedValue(Object.assign(new Error('x'), { status: 503 }));
  const { result } = renderHook(() => usePrices('admin-token', true, () => {}));

  await act(() => result.current.startRefresh());

  expect(result.current.refreshError).toMatch(/TWELVE_DATA_API_KEY/);
});

test('starting a refresh that is already running says so', async () => {
  stockService.startPriceRefresh.mockRejectedValue(Object.assign(new Error('x'), { status: 409 }));
  const { result } = renderHook(() => usePrices('admin-token', true, () => {}));

  await act(() => result.current.startRefresh());

  expect(result.current.refreshError).toBe('A price update is already running.');
});

test('while a refresh runs it polls, then reloads the prices when it finishes', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  stockService.startPriceRefresh.mockResolvedValue({ ...IDLE, running: true });
  stockService.getRefreshStatus
    .mockResolvedValueOnce({ ...IDLE, running: true })
    .mockResolvedValue({ ...IDLE, running: false, lastStored: 1 });
  const { result } = renderHook(() => usePrices('admin-token', true, () => {}));

  await act(() => result.current.startRefresh());
  expect(result.current.status.running).toBe(true);

  await act(() => vi.advanceTimersByTimeAsync(4000));

  await waitFor(() => expect(result.current.status.running).toBe(false));
  // Once after the start and once more after the run finished.
  expect(stockService.getLatestPrices.mock.calls.length).toBeGreaterThanOrEqual(2);
});
