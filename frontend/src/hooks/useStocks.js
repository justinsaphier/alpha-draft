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

  // Shared by add/edit/remove: clears the last error, runs the action, and on
  // failure records "<failurePrefix>: <reason>". Rethrows when the caller
  // (the form) needs the error to show field messages.
  async function runAction(action, failurePrefix, { rethrow = true } = {}) {
    setActionError(null);
    try {
      return await action();
    } catch (err) {
      setActionError(`${failurePrefix}: ${err.message}`);
      if (rethrow) {
        throw err;
      }
      return undefined;
    }
  }

  function addStock(stock) {
    return runAction(async () => {
      const created = await createStock(stock);
      setStocks((current) => [...current, created]);
    }, `Could not add ${stock.ticker}`);
  }

  function editStock(id, changes) {
    return runAction(async () => {
      const updated = await updateStock(id, changes);
      setStocks((current) => current.map((s) => (s.id === id ? updated : s)));
    }, `Could not update ${changes.ticker}`);
  }

  function removeStock(id) {
    return runAction(
      async () => {
        await deleteStock(id);
        setStocks((current) => current.filter((s) => s.id !== id));
      },
      'Could not delete stock',
      { rethrow: false },
    );
  }

  return { stocks, isLoading, error, actionError, addStock, editStock, removeStock };
}

export default useStocks;