import { useId } from 'react';
import { SECTORS } from '../constants/sectors.js';
import useStockFormFields from '../hooks/useStockFormFields.js';
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
  const { values, errors, setErrors, handleChange, reset } = useStockFormFields({
    ticker: initialStock.ticker,
    name: initialStock.name,
    sector: initialStock.sector,
  });
  const id = useId();

  async function handleSubmit(e) {
    e.preventDefault();
    const stock = { ticker: values.ticker.trim().toUpperCase(), name: values.name.trim(), sector: values.sector };

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
      reset(EMPTY_STOCK);
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
        value={values.ticker}
        onChange={handleChange('ticker')}
        required
        maxLength={MAX_TICKER_LENGTH}
        {...errorProps('ticker')}
      />
      {renderError('ticker')}
      <input
        aria-label="Company name"
        placeholder="Company name"
        value={values.name}
        onChange={handleChange('name')}
        required
        {...errorProps('name')}
      />
      {renderError('name')}
      <select aria-label="Sector" value={values.sector} onChange={handleChange('sector')} {...errorProps('sector')}>
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