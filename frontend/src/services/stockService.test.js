import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, createStock, signIn, signUp } from './stockService.js';

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
