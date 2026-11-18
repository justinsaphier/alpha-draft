import { useCallback, useEffect, useRef, useState } from 'react';
import { getLatestPrices, getRefreshStatus, startPriceRefresh } from '../services/stockService.js';

const POLL_MS = 4000;

// Explains a failed refresh start in plain words instead of the raw status code.
function refreshStartMessage(err) {
  if (err?.status === 409) {
    return 'A price update is already running.';
  }
  if (err?.status === 503) {
    return 'Price updates are off until the server has a TWELVE_DATA_API_KEY.';
  }
  return `Could not start the price update: ${err?.message ?? 'unknown error'}`;
}

// Closing prices for the stock pool, keyed by stock id, plus the status of the admin's refresh.
// While a refresh runs, the status is polled every few seconds; when it finishes, the prices are
// reloaded so the list updates without a page refresh.
function usePrices(token, isAdmin, onUnauthorized) {
  const [pricesByStockId, setPricesByStockId] = useState({});
  const [status, setStatus] = useState(null);
  const [refreshError, setRefreshError] = useState(null);

  const onUnauthorizedRef = useRef(onUnauthorized);
  useEffect(() => {
    onUnauthorizedRef.current = onUnauthorized;
  });

  const handleError = useCallback((err) => {
    if (err?.status === 401 && onUnauthorizedRef.current) {
      onUnauthorizedRef.current();
    }
  }, []);

  const loadPrices = useCallback(async () => {
    try {
      const list = await getLatestPrices(token);
      if (Array.isArray(list)) {
        setPricesByStockId(Object.fromEntries(list.map((price) => [price.stockId, price])));
      }
    } catch (err) {
      handleError(err);
    }
  }, [token, handleError]);

  const loadStatus = useCallback(async () => {
    try {
      const next = await getRefreshStatus(token);
      if (next) {
        setStatus(next);
      }
      return next;
    } catch (err) {
      handleError(err);
      return null;
    }
  }, [token, handleError]);

  // Prices are shown to everyone; the refresh status is only needed by admins.
  useEffect(() => {
    loadPrices();
    if (isAdmin) {
      loadStatus();
    }
  }, [loadPrices, loadStatus, isAdmin]);

  const isRunning = Boolean(status?.running);

  useEffect(() => {
    if (!isRunning) {
      return undefined;
    }
    const timer = setInterval(async () => {
      const next = await loadStatus();
      if (next && !next.running) {
        await loadPrices();
      }
    }, POLL_MS);
    return () => clearInterval(timer);
  }, [isRunning, loadStatus, loadPrices]);

  async function startRefresh() {
    setRefreshError(null);
    try {
      const next = await startPriceRefresh(token);
      setStatus(next ?? { running: true });
    } catch (err) {
      handleError(err);
      setRefreshError(refreshStartMessage(err));
      // A 409 means a run is already going, so start watching it.
      if (err?.status === 409) {
        loadStatus();
      }
    }
  }

  return { pricesByStockId, status, refreshError, startRefresh };
}

export default usePrices;
