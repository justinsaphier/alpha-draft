import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, createStock } from './stockService.js';

function mockFetchResponse({ ok, status, body, jsonFails = false }) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      ok,
      status,
      json: jsonFails ? () => Promise.reject(new Error('not JSON')) : () => Promise.resolve(body),
    }),
  );
}

const stock = { ticker: 'MSFT', name: 'Microsoft Corporation', sector: 'Technology' };

describe('stockService errors', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('returns the created stock on success', async () => {
    mockFetchResponse({ ok: true, status: 201, body: { id: 7, ...stock } });
    await expect(createStock(stock)).resolves.toEqual({ id: 7, ...stock });
  });

  it('throws an ApiError with per-field errors from a 400 response', async () => {
    mockFetchResponse({ ok: false, status: 400, body: { errors: { ticker: 'Ticker already exists.' } } });

    const error = await createStock(stock).catch((err) => err);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(400);
    expect(error.fieldErrors).toEqual({ ticker: 'Ticker already exists.' });
    expect(error.message).toBe('Please fix the highlighted fields.');
  });

  it('uses the backend message when one is sent', async () => {
    mockFetchResponse({ ok: false, status: 409, body: { message: 'That stock is already in the pool.' } });

    const error = await createStock(stock).catch((err) => err);

    expect(error.message).toBe('That stock is already in the pool.');
    expect(error.fieldErrors).toEqual({});
  });

  it('falls back to a generic message when the body is not JSON', async () => {
    mockFetchResponse({ ok: false, status: 500, jsonFails: true });

    const error = await createStock(stock).catch((err) => err);

    expect(error.message).toBe('Request failed: 500');
  });
});