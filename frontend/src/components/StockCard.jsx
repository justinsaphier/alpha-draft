import { formatPrice, formatTradeDate } from '../utils/format.js';

// One stock in the pool: ticker, company, sector, and its latest closing price when there is one.
// Admins also get Edit and Delete buttons (only passed in for admins).
function StockCard({ stock, price, owner, onEdit, onDelete }) {
  const hasPrice = price && formatPrice(price.closePrice);

  return (
    <li className="stock-card">
      <div className="stock-card__top">
        <span className="stock-card__ticker">{stock.ticker}</span>
        <span className="stock-card__sector">{stock.sector}</span>
      </div>
      {owner && <p className="stock-card__owner">Drafted by {owner}</p>}
      <p className="stock-card__name">{stock.name}</p>
      <div className="stock-card__bottom">
        {hasPrice ? (
          <span className="stock-card__price">
            <span className="stock-card__amount">{formatPrice(price.closePrice)}</span>
            <span className="stock-card__date">Close · {formatTradeDate(price.tradeDate)}</span>
          </span>
        ) : (
          <span className="stock-card__price stock-card__price--empty">No close yet</span>
        )}
        {(onEdit || onDelete) && (
          <span className="stock-card__actions">
            {onEdit && (
              <button type="button" className="button button--quiet" onClick={onEdit} aria-label={`Edit ${stock.ticker}`}>
                Edit
              </button>
            )}
            {onDelete && (
              <button type="button" className="button button--danger" onClick={onDelete} aria-label={`Delete ${stock.ticker}`}>
                Delete
              </button>
            )}
          </span>
        )}
      </div>
    </li>
  );
}

export default StockCard;
