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

function jsonOptions(method, body) {
  return {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  };
}

export async function getStocks() {
  const response = await request('/stocks');
  return response.json();
}

export async function createStock(stock) {
  const response = await request('/stocks', jsonOptions('POST', stock));
  return response.json();
}

export async function updateStock(id, stock) {
  const response = await request(`/stocks/${id}`, jsonOptions('PUT', stock));
  return response.json();
}

export async function deleteStock(id) {
  // DELETE returns 204 No Content, so there is no body to parse.
  await request(`/stocks/${id}`, { method: 'DELETE' });
}