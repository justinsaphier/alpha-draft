import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import useStockFormFields from './useStockFormFields.js';

const initial = { ticker: '', name: '', sector: 'Technology' };

function changeEvent(value) {
  return { target: { value } };
}

describe('useStockFormFields', () => {
  it('starts with the initial values and no errors', () => {
    const { result } = renderHook(() => useStockFormFields(initial));
    expect(result.current.values).toEqual(initial);
    expect(result.current.errors).toEqual({});
  });

  it('updates only the changed field', () => {
    const { result } = renderHook(() => useStockFormFields(initial));

    act(() => result.current.handleChange('ticker')(changeEvent('msft')));

    expect(result.current.values).toEqual({ ...initial, ticker: 'msft' });
  });

  it("clears only the changed field's error", () => {
    const { result } = renderHook(() => useStockFormFields(initial));
    act(() => result.current.setErrors({ ticker: 'Required', name: 'Required' }));

    act(() => result.current.handleChange('ticker')(changeEvent('M')));

    expect(result.current.errors.ticker).toBeUndefined();
    expect(result.current.errors.name).toBe('Required');
  });

  it('reset restores the initial values and clears errors', () => {
    const { result } = renderHook(() => useStockFormFields(initial));
    act(() => result.current.handleChange('name')(changeEvent('Microsoft')));
    act(() => result.current.setErrors({ name: 'Too long' }));

    act(() => result.current.reset());

    expect(result.current.values).toEqual(initial);
    expect(result.current.errors).toEqual({});
  });
});