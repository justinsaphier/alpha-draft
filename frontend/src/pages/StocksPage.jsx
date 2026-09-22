import { useEffect, useState } from 'react';
import StockList from '../components/StockList.jsx';
import { getStocks } from '../services/stockService.js';

function StocksPage() {
  const [stocks, setStocks] = useState([]);
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    getStocks()
      .then(setStocks)
      .catch((err) => setError(err.message))
      .finally(() => setIsLoading(false));
  }, []);

  return (
    <main>
      <h1>Alpha Draft — Stock Pool</h1>
      {isLoading && <p>Loading stocks…</p>}
      {error && <p role="alert">Could not load stocks: {error}</p>}
      {!isLoading && !error && <StockList stocks={stocks} />}
    </main>
  );
}

export default StocksPage;
