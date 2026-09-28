import { useEffect, useState } from 'react';
import StockForm from '../components/StockForm.jsx';
import StockList from '../components/StockList.jsx';
import { createStock, getStocks } from '../services/stockService.js';

function StocksPage() {
  const [stocks, setStocks] = useState([]);
  const [error, setError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    getStocks()
      .then(setStocks)
      .catch((err) => setError(err.message))
      .finally(() => setIsLoading(false));
  }, []);

  // Each handler updates local state from the server's response instead of
  // re-fetching, so the list changes immediately with no page refresh.
  async function handleCreate(stock) {
    setActionError(null);
    try {
      const created = await createStock(stock);
      setStocks((current) => [...current, created]);
    } catch (err) {
      setActionError(`Could not add ${stock.ticker}: ${err.message}`);
      throw err;
    }
  }

  return (
    <main>
      <h1>Alpha Draft — Stock Pool</h1>
      <section className="stock-form-section">
        <h2>Add a stock</h2>
        <StockForm onSubmit={handleCreate} />
      </section>
      {actionError && (
        <p role="alert" className="error">
          {actionError}
        </p>
      )}
      {isLoading && <p>Loading stocks…</p>}
      {error && <p role="alert">Could not load stocks: {error}</p>}
      {!isLoading && !error && <StockList stocks={stocks} />}
    </main>
  );
}

export default StocksPage;