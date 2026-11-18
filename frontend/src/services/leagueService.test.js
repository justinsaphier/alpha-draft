import { afterEach, expect, test, vi } from 'vitest';
import { getLeague, pickStock, saveCalls } from './leagueService.js';

afterEach(() => {
  vi.restoreAllMocks();
});

function respond(status, body) {
  return vi.spyOn(globalThis, 'fetch').mockResolvedValue(
    new Response(body === undefined ? null : JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  );
}

test('sends the token and returns the league summary', async () => {
  const fetchSpy = respond(200, { name: 'Alpha Draft League' });

  const summary = await getLeague('player-token');

  expect(summary.name).toBe('Alpha Draft League');
  const [url, options] = fetchSpy.mock.calls[0];
  expect(url).toBe('http://localhost:8080/league');
  expect(options.headers.Authorization).toBe('Bearer player-token');
});

test('shows the backend message for a rule the league enforces', async () => {
  respond(409, { message: 'It is not your pick.' });

  await expect(pickStock('AAPL', 'token')).rejects.toMatchObject({
    status: 409,
    message: 'It is not your pick.',
  });
});

test('sends calls as JSON with the 2x pick', async () => {
  const fetchSpy = respond(200, { month: 0, picks: [] });

  await saveCalls(0, { AAPL: 'LONG', JPM: 'SHORT' }, 'AAPL', 'token');

  const [url, options] = fetchSpy.mock.calls[0];
  expect(url).toBe('http://localhost:8080/league/calls/0');
  expect(options.method).toBe('PUT');
  expect(JSON.parse(options.body)).toEqual({ directions: { AAPL: 'LONG', JPM: 'SHORT' }, levered: 'AAPL' });
});

test('a 204 response resolves to nothing', async () => {
  respond(204);
  const { acceptTeam } = await import('./leagueService.js');

  await expect(acceptTeam(3, 'admin-token')).resolves.toBeNull();
});
