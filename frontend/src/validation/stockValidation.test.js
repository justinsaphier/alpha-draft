import { describe, expect, it } from 'vitest';
import { SECTORS } from '../constants/sectors.js';
import { validateStock } from './stockValidation.js';

const valid = { ticker: 'MSFT', name: 'Microsoft Corporation', sector: SECTORS[0] };

describe('validateStock', () => {
  it('returns no errors for a valid stock', () => {
    expect(validateStock(valid)).toEqual({});
  });

  it('requires a ticker and a company name', () => {
    const errors = validateStock({ ...valid, ticker: '', name: '' });
    expect(errors.ticker).toBe('Ticker is required.');
    expect(errors.name).toBe('Company name is required.');
  });

  it('rejects tickers with invalid characters', () => {
    expect(validateStock({ ...valid, ticker: 'AB$C' }).ticker).toMatch(/only letters, numbers/);
  });

  it('rejects tickers that start with a number', () => {
    expect(validateStock({ ...valid, ticker: '1ABC' }).ticker).toMatch(/start with a letter/);
  });

  it('allows tickers with a dot or dash', () => {
    expect(validateStock({ ...valid, ticker: 'BRK.B' })).toEqual({});
    expect(validateStock({ ...valid, ticker: 'RDS-A' })).toEqual({});
  });

  it('rejects names over 100 characters', () => {
    expect(validateStock({ ...valid, name: 'x'.repeat(101) }).name).toMatch(/100 characters or fewer/);
  });

  it('rejects a sector that is not in the list', () => {
    expect(validateStock({ ...valid, sector: 'Crypto' }).sector).toBe('Choose a sector from the list.');
  });
});