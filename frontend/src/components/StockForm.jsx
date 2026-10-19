import { useId, useState } from 'react';
import { SECTORS } from '../constants/sectors.js';
import { MAX_TICKER_LENGTH, validateStock } from '../validation/stockValidation.js';

const EMPTY_STOCK = { ticker: '', name: '', sector: SECTORS[0] };
const FORM_FIELDS = ['ticker', 'name', 'sector'];

// Keeps only the backend errors for fields this form actually shows.
function backendFieldErrors(err) {
  const fieldErrors = err?.fieldErrors ?? {};
  return Object.fromEntries(
    FORM_FIELDS.filter((field) => fieldErrors[field]).map((field) => [field, fieldErrors[field]]),
  );
}

// Used both to create a new stock and to edit an existing one in the list.
// Validates on submit and only calls onSubmit when every field is valid.
function StockForm({ initialStock = EMPTY_STOCK, submitLabel = 'Add Stock', onSubmit, onCancel }) {
  const [ticker, setTicker] = useState(initialStock.ticker);
  const [name, setName] = useState(initialStock.name);
  const [sector, setSector] = useState(initialStock.sector);
  const [errors, setErrors] = useState({});
  const id = useId();

  // Hide a field's error as soon as the user starts fixing it.
  function clearError(field) {
    setErrors((current) => ({ ...current, [field]: undefined }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const stock = { ticker: ticker.trim().toUpperCase(), name: name.trim(), sector };

    const validationErrors = validateStock(stock);
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    try {
      await onSubmit(stock);
    } catch (err) {
      // The page shows the overall error; keep what the user typed so they can retry.
      // If the backend rejected specific fields, show those messages under the inputs.
      setErrors(backendFieldErrors(err));
      return;
    }

    // Clear the create form after a successful add; edit mode is closed by the parent.
    if (!onCancel) {
      setTicker('');
      setName('');
      setSector(SECTORS[0]);
    }
  }

  function errorProps(field) {
    return {
      'aria-invalid': Boolean(errors[field]),
      'aria-describedby': errors[field] ? `${id}-${field}-error` : undefined,
    };
  }

  function renderError(field) {
    return errors[field] ? (
      <p id={`${id}-${field}-error`} className="field-error" role="alert">
        {errors[field]}
      </p>
    ) : null;
  }

  return (
    <form className="stock-form" onSubmit={handleSubmit} noValidate>
      <input
        aria-label="Ticker"
        placeholder="Ticker"
        value={ticker}
        onChange={(e) => {
          setTicker(e.target.value);
          clearError('ticker');
        }}
        required
        maxLength={MAX_TICKER_LENGTH}
        {...errorProps('ticker')}
      />
      {renderError('ticker')}
      <input
        aria-label="Company name"
        placeholder="Company name"
        value={name}
        onChange={(e) => {
          setName(e.target.value);
          clearError('name');
        }}
        required
        {...errorProps('name')}
      />
      {renderError('name')}
      <select
        aria-label="Sector"
        value={sector}
        onChange={(e) => {
          setSector(e.target.value);
          clearError('sector');
        }}
        {...errorProps('sector')}
      >
        {SECTORS.map((s) => (
          <option key={s} value={s}>
            {s}
          </option>
        ))}
      </select>
      {renderError('sector')}
      <button type="submit">{submitLabel}</button>
      {onCancel && (
        <button type="button" onClick={onCancel}>
          Cancel
        </button>
      )}
    </form>
  );
}

export default StockForm;