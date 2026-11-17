const API_BASE_URL = 'http://localhost:8080';

// Thrown when the backend rejects a request. Carries the HTTP status and,
// for validation failures, a { field: message } map from the response body.
export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

// Turns a failed response into an ApiError. Expects validation errors as
// { "errors": { "ticker": "...", "name": "..." } } and/or a "message",
// and falls back to a generic message if the body isn't JSON.
async function toApiError(response) {
  let body = null;
  try {
    body = await response.json();
  } catch {
    // No JSON body; use the generic message below.
  }

  const fieldErrors = body && body.errors && typeof body.errors === 'object' ? body.errors : {};
  const hasFieldErrors = Object.keys(fieldErrors).length > 0;
  const message =
    (body && typeof body.message === 'string' && body.message) ||
    (hasFieldErrors ? 'Please fix the highlighted fields.' : `Request failed: ${response.status}`);

  return new ApiError(response.status, message, fieldErrors);
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, options);

  if (!response.ok) {
    throw await toApiError(response);
  }

  return response;
}

// Adds "Authorization: Bearer <token>" when there is a token. The backend needs it on every
// /stocks call: any signed-in user may read, only an ADMIN may create, edit or delete.
function authHeaders(token) {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function jsonOptions(method, body, token) {
  return {
    method,
    headers: { 'Content-Type': 'application/json', ...authHeaders(token) },
    body: JSON.stringify(body),
  };
}

export async function getStocks(token) {
  const response = await request('/stocks', { headers: authHeaders(token) });
  return response.json();
}

export async function createStock(stock, token) {
  const response = await request('/stocks', jsonOptions('POST', stock, token));
  return response.json();
}

export async function updateStock(id, stock, token) {
  const response = await request(`/stocks/${id}`, jsonOptions('PUT', stock, token));
  return response.json();
}

export async function deleteStock(id, token) {
  // DELETE returns 204 No Content, so there is no body to parse.
  await request(`/stocks/${id}`, { method: 'DELETE', headers: authHeaders(token) });
}

// Sign-up and sign-in go through the backend only; React never talks to Supabase.
// Each resolves to { token, email, role } on success.
export async function signUp(credentials) {
  const response = await request('/auth/signup', jsonOptions('POST', credentials));
  return response.json();
}

export async function signIn(credentials) {
  const response = await request('/auth/signin', jsonOptions('POST', credentials));
  return response.json();
}