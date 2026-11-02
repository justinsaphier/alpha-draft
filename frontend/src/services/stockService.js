// Set VITE_API_BASE_URL in a .env file to point at another backend;
// defaults to the local Spring Boot server.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

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

// Builds an ApiError from a parsed error body (or null if the body wasn't JSON).
// Expects { "errors": { "ticker": "..." } } and/or { "message": "..." }.
export function parseErrorBody(status, body) {
  const fieldErrors = body?.errors && typeof body.errors === 'object' ? body.errors : {};
  const hasFieldErrors = Object.keys(fieldErrors).length > 0;
  const backendMessage = typeof body?.message === 'string' ? body.message : '';
  const message =
    backendMessage || (hasFieldErrors ? 'Please fix the highlighted fields.' : `Request failed: ${status}`);

  return new ApiError(status, message, fieldErrors);
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, options);

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw parseErrorBody(response.status, body);
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