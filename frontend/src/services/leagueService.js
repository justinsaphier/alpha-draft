import { ApiError } from './stockService.js';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Sends one request to the league API. A failure becomes an ApiError whose message is the
// backend's { "message" } (for example "It is not your pick.") so the page can show it as is.
async function call(path, { method = 'GET', token, body } = {}) {
  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  const options = { method, headers };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }
  const response = await fetch(`${API_BASE_URL}${path}`, options);
  if (!response.ok) {
    let data = null;
    try {
      data = await response.json();
    } catch {
      // No JSON body; the generic message below is used.
    }
    throw new ApiError(response.status, (data && data.message) || `Request failed: ${response.status}`);
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

export const getLeague = (token) => call('/league', { token });
export const joinLeague = (teamName, token) => call('/league/join', { method: 'POST', token, body: { teamName } });
export const getDraft = (token) => call('/league/draft', { token });
export const pickStock = (ticker, token) => call('/league/draft/picks', { method: 'POST', token, body: { ticker } });
export const getOwners = (token) => call('/league/owners', { token });
export const getStandings = (token) => call('/league/standings', { token });
export const getCalls = (month, token) => call(`/league/calls/${month}`, { token });
export const saveCalls = (month, directions, levered, token) =>
  call(`/league/calls/${month}`, { method: 'PUT', token, body: { directions, levered } });
export const submitCalls = (month, token) => call(`/league/calls/${month}/submit`, { method: 'POST', token });
export const getTeamCalls = (teamId, month, token) => call(`/league/teams/${teamId}/calls/${month}`, { token });

export const getAdminLeague = (token) => call('/admin/league', { token });
export const updateLeagueSettings = (settings, token) =>
  call('/admin/league/settings', { method: 'PUT', token, body: settings });
export const acceptTeam = (id, token) => call(`/admin/league/teams/${id}/accept`, { method: 'POST', token });
export const rejectTeam = (id, token) => call(`/admin/league/teams/${id}/reject`, { method: 'POST', token });
export const startDraftNow = (token) => call('/admin/league/draft/start', { method: 'POST', token });
export const startSeason = (token) => call('/admin/league/season/start', { method: 'POST', token });
export const finalizeMonth = (token) => call('/admin/league/months/finalize', { method: 'POST', token });
export const setClosePrice = (ticker, month, close, token) =>
  call(`/admin/league/snapshots/${ticker}/${month}`, { method: 'PUT', token, body: { close } });
export const clearClosePrice = (ticker, month, token) =>
  call(`/admin/league/snapshots/${ticker}/${month}`, { method: 'DELETE', token });
