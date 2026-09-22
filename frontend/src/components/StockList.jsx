import StockCard from './StockCard.jsx';

function StockList({ stocks }) {
  if (stocks.length === 0) {
    return <p>No stocks available.</p>;
  }

  return (
    <ul className="stock-list">
      {stocks.map((stock) => (
        <StockCard key={stock.id} stock={stock} />
      ))}
    </ul>
  );
}

export default StockList;
