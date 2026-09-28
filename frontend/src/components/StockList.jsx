import { useState } from 'react';
import StockCard from './StockCard.jsx';
import StockForm from './StockForm.jsx';

function StockList({ stocks, onUpdate, onDelete }) {
  const [editingId, setEditingId] = useState(null);

  if (stocks.length === 0) {
    return <p>No stocks available.</p>;
  }

  async function handleSave(id, changes) {
    await onUpdate(id, changes);
    setEditingId(null);
  }

  return (
    <ul className="stock-list">
      {stocks.map((stock) =>
        stock.id === editingId ? (
          <li key={stock.id} className="stock-card stock-card--editing">
            <StockForm
              initialStock={stock}
              submitLabel="Save"
              onSubmit={(changes) => handleSave(stock.id, changes)}
              onCancel={() => setEditingId(null)}
            />
          </li>
        ) : (
          <StockCard
            key={stock.id}
            stock={stock}
            onEdit={onUpdate && (() => setEditingId(stock.id))}
            onDelete={onDelete && (() => onDelete(stock.id))}
          />
        ),
      )}
    </ul>
  );
}

export default StockList;