import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, createStock, deleteStock, getStocks, signIn, signUp, updateStock } from './stockService.js';

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
describe('stockService sign-up and sign-in', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('posts credentials to /auth/signin and returns the token, email and role', async () => {
    mockFetchResponse({ ok: true, status: 200, body: { token: 't', email: 'p@example.com', role: 'PLAYER' } });

    const result = await signIn({ email: 'p@example.com', password: 'correct horse' });

    expect(result).toEqual({ token: 't', email: 'p@example.com', role: 'PLAYER' });
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/auth/signin',
      expect.objectContaining({ method: 'POST', body: JSON.stringify({ email: 'p@example.com', password: 'correct horse' }) }),
    );
  });

  it('posts credentials to /auth/signup', async () => {
    mockFetchResponse({ ok: true, status: 201, body: { token: 't', email: 'p@example.com', role: 'PLAYER' } });

    await signUp({ email: 'p@example.com', password: 'correct horse' });

    expect(fetch).toHaveBeenCalledWith('http://localhost:8080/auth/signup', expect.objectContaining({ method: 'POST' }));
  });

  it('throws an ApiError with the backend message for a wrong password', async () => {
    mockFetchResponse({ ok: false, status: 401, body: { message: 'Email or password is incorrect.' } });

    const error = await signIn({ email: 'p@example.com', password: 'nope' }).catch((err) => err);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(401);
    expect(error.message).toBe('Email or password is incorrect.');
  });

  it('carries the duplicate-email field error from a 409 response', async () => {
    mockFetchResponse({ ok: false, status: 409, body: { errors: { email: 'An account with this email already exists.' } } });

    const error = await signUp({ email: 'p@example.com', password: 'correct horse' }).catch((err) => err);

    expect(error.fieldErrors).toEqual({ email: 'An account with this email already exists.' });
  });
});


describe('stockService sends the sign-in token', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  const bearer = { Authorization: 'Bearer t' };

  it('adds the bearer token to every stock request', async () => {
    mockFetchResponse({ ok: true, status: 200, body: [] });

    await getStocks('t');
    await createStock(stock, 't');
    await updateStock(7, stock, 't');
    await deleteStock(7, 't');

    expect(fetch).toHaveBeenNthCalledWith(1, 'http://localhost:8080/stocks', { headers: bearer });
    expect(fetch).toHaveBeenNthCalledWith(
      2,
      'http://localhost:8080/stocks',
      expect.objectContaining({ method: 'POST', headers: { 'Content-Type': 'application/json', ...bearer } }),
    );
    expect(fetch).toHaveBeenNthCalledWith(
      3,
      'http://localhost:8080/stocks/7',
      expect.objectContaining({ method: 'PUT', headers: { 'Content-Type': 'application/json', ...bearer } }),
    );
    expect(fetch).toHaveBeenNthCalledWith(4, 'http://localhost:8080/stocks/7', { method: 'DELETE', headers: bearer });
  });

  it('sends no Authorization header without a token', async () => {
    mockFetchResponse({ ok: true, status: 200, body: [] });

    await getStocks();

    expect(fetch).toHaveBeenCalledWith('http://localhost:8080/stocks', { headers: {} });
  });

  it('surfaces a 403 from the backend as an ApiError', async () => {
    mockFetchResponse({ ok: false, status: 403, jsonFails: true });

    const error = await deleteStock(7, 't').catch((err) => err);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(403);
  });
});