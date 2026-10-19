import { SECTORS } from '../constants/sectors.js';

export const MAX_TICKER_LENGTH = 10;
export const MAX_NAME_LENGTH = 100;

// Starts with a letter; then letters, numbers, "." or "-" (e.g. BRK.B, RDS-A).
const TICKER_PATTERN = /^[A-Z][A-Z0-9.-]*$/;

// Checks a stock before it is sent to the backend.
// Expects already-cleaned values (trimmed, ticker uppercased).
// Returns { field: message } for each problem; an empty object means valid.
export function validateStock({ ticker, name, sector }) {
  const errors = {};

  if (!ticker) {
    errors.ticker = 'Ticker is required.';
  } else if (ticker.length > MAX_TICKER_LENGTH) {
    errors.ticker = `Ticker must be ${MAX_TICKER_LENGTH} characters or fewer.`;
  } else if (!TICKER_PATTERN.test(ticker)) {
    errors.ticker = 'Ticker must start with a letter and use only letters, numbers, "." or "-".';
  }

  if (!name) {
    errors.name = 'Company name is required.';
  } else if (name.length > MAX_NAME_LENGTH) {
    errors.name = `Company name must be ${MAX_NAME_LENGTH} characters or fewer.`;
  }

  if (!SECTORS.includes(sector)) {
    errors.sector = 'Choose a sector from the list.';
  }

  return errors;
}