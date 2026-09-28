function StockCard({ stock, onEdit, onDelete }) {
  return (
    <li className="stock-card">
      <span className="stock-card__ticker">{stock.ticker}</span>
      <span className="stock-card__name">{stock.name}</span>
      <span className="stock-card__sector">{stock.sector}</span>
      {(onEdit || onDelete) && (
        <span className="stock-card__actions">
          {onEdit && (
            <button type="button" onClick={onEdit} aria-label={`Edit ${stock.ticker}`}>
              Edit
            </button>
          )}
          {onDelete && (
            <button type="button" onClick={onDelete} aria-label={`Delete ${stock.ticker}`}>
              Delete
            </button>
          )}
        </span>
      )}
    </li>
  );
}

export default StockCard;