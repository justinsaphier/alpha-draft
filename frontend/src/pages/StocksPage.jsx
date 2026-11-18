import { useAuth } from '../auth/AuthContext.jsx';
import PriceRefreshPanel from '../components/PriceRefreshPanel.jsx';
import StockForm from '../components/StockForm.jsx';
import StockList from '../components/StockList.jsx';
import useStocks from '../hooks/useStocks.js';
import usePrices from '../hooks/usePrices.js';
import { getOwners } from '../services/leagueService.js';
import { useEffect, useState } from 'react';

// The backend only shows the stock pool to signed-in users. Keying the pool by token gives
// each signed-in user a fresh list, so signing out and in as someone else never shows the
// previous user's data.
function StocksPage() {
  const { isSignedIn, isAdmin, token, signOut } = useAuth();

  return (
    <main className="page">
      {isSignedIn ? (
        <StockPool key={token} token={token} isAdmin={isAdmin} onUnauthorized={signOut} />
      ) : (
        <section className="welcome">
          <h1>Stock pool</h1>
          <p>Sign in to see the stock pool.</p>
        </section>
      )}
    </main>
  );
}

// Layout only: stock data lives in useStocks, price data in usePrices.
// Only admins get the add form, the price refresh and the Edit/Delete buttons. Hiding them is a
// convenience: the backend still rejects a player's create, edit, delete or refresh with 403.
function StockPool({ token, isAdmin, onUnauthorized }) {
  const { stocks, isLoading, error, actionError, addStock, editStock, removeStock } = useStocks(
    token,
    onUnauthorized,
  );
  const { pricesByStockId, status, refreshError, startRefresh } = usePrices(token, isAdmin, onUnauthorized);
  const [owners, setOwners] = useState({});
  useEffect(() => {
    getOwners(token)
      .then((rows) => {
        if (Array.isArray(rows)) setOwners(Object.fromEntries(rows.map((r) => [r.ticker, r.teamName])));
      })
      .catch(() => {});
  }, [token]);

  return (
    <>
      <header className="page-header">
        <h1>Stock pool</h1>
        <p>Every S&amp;P 500 company, with its latest closing price.</p>
      </header>

      {isAdmin && (
        <div className="admin-grid">
          <section className="panel" aria-labelledby="add-stock-title">
            <h2 id="add-stock-title">Add a stock</h2>
            <StockForm onSubmit={addStock} />
          </section>
          <PriceRefreshPanel status={status} error={refreshError} onRefresh={startRefresh} />
        </div>
      )}

      {actionError && (
        <p role="alert" className="notice notice--error">
          {actionError}
        </p>
      )}
      {isLoading && (
        <p className="loading" aria-busy="true">
          Loading stocks…
        </p>
      )}
      {error && (
        <p role="alert" className="notice notice--error">
          Could not load stocks: {error}
        </p>
      )}
      {!isLoading && !error && (
        <StockList
          stocks={stocks}
          prices={pricesByStockId}
          owners={owners}
          onUpdate={isAdmin ? editStock : undefined}
          onDelete={isAdmin ? removeStock : undefined}
        />
      )}
    </>
  );
}

export default StocksPage;
