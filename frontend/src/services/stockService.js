const API_BASE_URL = 'http://localhost:8080';

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, options);

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
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