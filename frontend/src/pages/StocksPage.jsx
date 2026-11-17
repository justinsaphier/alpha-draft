import { useAuth } from '../auth/AuthContext.jsx';
import StockForm from '../components/StockForm.jsx';
import StockList from '../components/StockList.jsx';
import useStocks from '../hooks/useStocks.js';

// The backend only shows the stock pool to signed-in users. Keying the pool by token gives
// each signed-in user a fresh list, so signing out and in as someone else never shows the
// previous user's data.
function StocksPage() {
  const { isSignedIn, isAdmin, token } = useAuth();

  return (
    <main>
      <h1>Alpha Draft — Stock Pool</h1>
      {isSignedIn ? <StockPool key={token} token={token} isAdmin={isAdmin} /> : <p>Sign in to see the stock pool.</p>}
    </main>
  );
}

// Layout only: all stock data and operations live in the useStocks hook.
// Only admins get the add form and the Edit/Delete buttons. Hiding them is a convenience:
// the backend still rejects a player's create, edit or delete with 403.
function StockPool({ token, isAdmin }) {
  const { stocks, isLoading, error, actionError, addStock, editStock, removeStock } = useStocks(token);

  return (
    <>
      {isAdmin && (
        <section className="stock-form-section">
          <h2>Add a stock</h2>
          <StockForm onSubmit={addStock} />
        </section>
      )}
      {actionError && (
        <p role="alert" className="error">
          {actionError}
        </p>
      )}
      {isLoading && <p>Loading stocks…</p>}
      {error && <p role="alert">Could not load stocks: {error}</p>}
      {!isLoading && !error && (
        <StockList
          stocks={stocks}
          onUpdate={isAdmin ? editStock : undefined}
          onDelete={isAdmin ? removeStock : undefined}
        />
      )}
    </>
  );
}

export default StocksPage;