import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import useStocks from './useStocks.js';
import * as stockService from '../services/stockService.js';

vi.mock('../services/stockService.js', () => ({
  getStocks: vi.fn(),
  createStock: vi.fn(),
  updateStock: vi.fn(),
  deleteStock: vi.fn(),
}));

const apple = { id: 1, ticker: 'AAPL', name: 'Apple Inc.', sector: 'Technology' };
const msft = { id: 2, ticker: 'MSFT', name: 'Microsoft Corporation', sector: 'Technology' };

async function renderLoadedHook() {
  const hook = renderHook(() => useStocks());
  await waitFor(() => expect(hook.result.current.isLoading).toBe(false));
  return hook;
}

describe('useStocks', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    stockService.getStocks.mockResolvedValue([apple]);
  });

  it('loads stocks on mount', async () => {
    const { result } = await renderLoadedHook();
    expect(result.current.stocks).toEqual([apple]);
    expect(result.current.error).toBeNull();
  });

  it('records a load error', async () => {
    stockService.getStocks.mockRejectedValue(new Error('Network down'));
    const { result } = await renderLoadedHook();
    expect(result.current.error).toBe('Network down');
  });

  it('adds a stock to the list', async () => {
    stockService.createStock.mockResolvedValue(msft);
    const { result } = await renderLoadedHook();

    await act(() => result.current.addStock({ ticker: 'MSFT' }));

    expect(result.current.stocks).toEqual([apple, msft]);
    expect(result.current.actionError).toBeNull();
  });

  it('sets actionError and rethrows when add fails', async () => {
    stockService.createStock.mockRejectedValue(new Error('Ticker already exists.'));
    const { result } = await renderLoadedHook();

    await act(async () => {
      await expect(result.current.addStock({ ticker: 'AAPL' })).rejects.toThrow('Ticker already exists.');
    });

    expect(result.current.actionError).toBe('Could not add AAPL: Ticker already exists.');
  });

  it('replaces an edited stock', async () => {
    const renamed = { ...apple, name: 'Apple' };
    stockService.updateStock.mockResolvedValue(renamed);
    const { result } = await renderLoadedHook();

    await act(() => result.current.editStock(1, renamed));

    expect(result.current.stocks).toEqual([renamed]);
  });

  it('sets actionError and rethrows when edit fails', async () => {
    stockService.updateStock.mockRejectedValue(new Error('Bad sector'));
    const { result } = await renderLoadedHook();

    await act(async () => {
      await expect(result.current.editStock(1, { ticker: 'AAPL' })).rejects.toThrow('Bad sector');
    });

    expect(result.current.actionError).toBe('Could not update AAPL: Bad sector');
  });

  it('removes a deleted stock', async () => {
    stockService.deleteStock.mockResolvedValue(undefined);
    const { result } = await renderLoadedHook();

    await act(() => result.current.removeStock(1));

    expect(result.current.stocks).toEqual([]);
  });

  it('sets actionError without throwing when delete fails', async () => {
    stockService.deleteStock.mockRejectedValue(new Error('Not found'));
    const { result } = await renderLoadedHook();

    await act(() => result.current.removeStock(1));

    expect(result.current.actionError).toBe('Could not delete stock: Not found');
    expect(result.current.stocks).toEqual([apple]);
  });

  it('clears the previous actionError when a new action succeeds', async () => {
    stockService.deleteStock.mockRejectedValueOnce(new Error('Not found'));
    stockService.createStock.mockResolvedValue(msft);
    const { result } = await renderLoadedHook();

    await act(() => result.current.removeStock(1));
    await act(() => result.current.addStock({ ticker: 'MSFT' }));

    expect(result.current.actionError).toBeNull();
  });
});