import { afterEach, describe, expect, it, vi } from 'vitest';
import { API_BASE_URL, ApiError, deleteStock, getStocks, parseErrorBody, updateStock } from './stockService.js';

describe('parseErrorBody', () => {
  it('returns field errors with the default message', () => {
    const error = parseErrorBody(400, { errors: { name: 'Company name is required.' } });

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(400);
    expect(error.fieldErrors).toEqual({ name: 'Company name is required.' });
    expect(error.message).toBe('Please fix the highlighted fields.');
  });

  it('prefers the backend message over the default', () => {
    const error = parseErrorBody(400, { message: 'Bad stock.', errors: { ticker: 'Too long.' } });
    expect(error.message).toBe('Bad stock.');
    expect(error.fieldErrors).toEqual({ ticker: 'Too long.' });
  });

  it('uses a generic message for a null body', () => {
    const error = parseErrorBody(503, null);
    expect(error.message).toBe('Request failed: 503');
    expect(error.fieldErrors).toEqual({});
  });

  it('ignores a non-object errors field and non-string message', () => {
    const error = parseErrorBody(400, { errors: 'oops', message: 42 });
    expect(error.fieldErrors).toEqual({});
    expect(error.message).toBe('Request failed: 400');
  });
});

describe('stockService requests', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('defaults the API base URL to the local backend', () => {
    expect(API_BASE_URL).toBe('http://localhost:8080');
  });

  it('getStocks fetches /stocks and returns the list', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve([{ id: 1 }]) });
    vi.stubGlobal('fetch', fetchMock);

    await expect(getStocks()).resolves.toEqual([{ id: 1 }]);
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/stocks', {});
  });

  it('updateStock sends a PUT with a JSON body', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve({ id: 3 }) });
    vi.stubGlobal('fetch', fetchMock);

    await updateStock(3, { ticker: 'NVDA' });

    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('http://localhost:8080/stocks/3');
    expect(options.method).toBe('PUT');
    expect(JSON.parse(options.body)).toEqual({ ticker: 'NVDA' });
  });

  it('deleteStock throws an ApiError on a 404', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 404, json: () => Promise.reject(new Error('no body')) }),
    );

    const error = await deleteStock(9).catch((err) => err);
    expect(error).toBeInstanceOf(ApiError);
    expect(error.message).toBe('Request failed: 404');
  });
});