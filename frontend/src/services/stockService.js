const API_BASE_URL = 'http://localhost:8080';

export async function getStocks() {
  const response = await fetch(`${API_BASE_URL}/stocks`);

  if (!response.ok) {
    throw new Error(`Failed to fetch stocks: ${response.status}`);
  }

  return response.json();
}
