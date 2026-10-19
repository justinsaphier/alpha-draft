import { useEffect, useState } from 'react';
import { createStock, deleteStock, getStocks, updateStock } from '../services/stockService.js';

// Owns the stock list and every operation on it, so pages only handle layout.
// Each operation updates local state from the server's response instead of
// re-fetching, so the list changes immediately with no page refresh.
function useStocks() {
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

  async function addStock(stock) {
    setActionError(null);
    try {
      const created = await createStock(stock);
      setStocks((current) => [...current, created]);
    } catch (err) {
      setActionError(`Could not add ${stock.ticker}: ${err.message}`);
      throw err;
    }
  }

  async function editStock(id, changes) {
    setActionError(null);
    try {
      const updated = await updateStock(id, changes);
      setStocks((current) => current.map((s) => (s.id === id ? updated : s)));
    } catch (err) {
      setActionError(`Could not update ${changes.ticker}: ${err.message}`);
      throw err;
    }
  }

  async function removeStock(id) {
    setActionError(null);
    try {
      await deleteStock(id);
      setStocks((current) => current.filter((s) => s.id !== id));
    } catch (err) {
      setActionError(`Could not delete stock: ${err.message}`);
    }
  }

  return { stocks, isLoading, error, actionError, addStock, editStock, removeStock };
}

export default useStocks;