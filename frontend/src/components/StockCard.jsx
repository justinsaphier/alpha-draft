function StockCard({ stock }) {
  return (
    <li className="stock-card">
      <span className="stock-card__ticker">{stock.ticker}</span>
      <span className="stock-card__name">{stock.name}</span>
      <span className="stock-card__sector">{stock.sector}</span>
    </li>
  );
}

export default StockCard;
