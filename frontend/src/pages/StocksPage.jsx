import StockForm from '../components/StockForm.jsx';
import StockList from '../components/StockList.jsx';
import useStocks from '../hooks/useStocks.js';

// Layout only: all stock data and operations live in the useStocks hook.
function StocksPage() {
  const { stocks, isLoading, error, actionError, addStock, editStock, removeStock } = useStocks();

  return (
    <main>
      <h1>Alpha Draft — Stock Pool</h1>
      <section className="stock-form-section">
        <h2>Add a stock</h2>
        <StockForm onSubmit={addStock} />
      </section>
      {actionError && (
        <p role="alert" className="error">
          {actionError}
        </p>
      )}
      {isLoading && <p>Loading stocks…</p>}
      {error && <p role="alert">Could not load stocks: {error}</p>}
      {!isLoading && !error && <StockList stocks={stocks} onUpdate={editStock} onDelete={removeStock} />}
    </main>
  );
}

export default StocksPage;