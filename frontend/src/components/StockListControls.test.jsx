import { fireEvent, render, screen } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import StockListControls from './StockListControls.jsx';
import {
  DEFAULT_FILTER,
  DEFAULT_SORT,
  FILTER_STRATEGIES,
  SORT_STRATEGIES,
} from '../strategies/stockListStrategies.js';

function renderControls(overrides = {}) {
  const props = {
    sortKey: DEFAULT_SORT,
    filterKey: DEFAULT_FILTER,
    onSortChange: vi.fn(),
    onFilterChange: vi.fn(),
    ...overrides,
  };
  render(<StockListControls {...props} />);
  return props;
}

test('renders one option per sort and filter strategy', () => {
  renderControls();

  expect(screen.getByLabelText('Sort by').options).toHaveLength(Object.keys(SORT_STRATEGIES).length);
  expect(screen.getByLabelText('Filter by sector').options).toHaveLength(Object.keys(FILTER_STRATEGIES).length);
});

test('shows the selected sort and filter keys', () => {
  renderControls();

  expect(screen.getByLabelText('Sort by')).toHaveValue(DEFAULT_SORT);
  expect(screen.getByLabelText('Filter by sector')).toHaveValue(DEFAULT_FILTER);
});

test('reports a new sort key', () => {
  const otherSort = Object.keys(SORT_STRATEGIES).find((key) => key !== DEFAULT_SORT);
  const { onSortChange, onFilterChange } = renderControls();

  fireEvent.change(screen.getByLabelText('Sort by'), { target: { value: otherSort } });

  expect(onSortChange).toHaveBeenCalledWith(otherSort);
  expect(onFilterChange).not.toHaveBeenCalled();
});

test('reports a new filter key', () => {
  const otherFilter = Object.keys(FILTER_STRATEGIES).find((key) => key !== DEFAULT_FILTER);
  const { onSortChange, onFilterChange } = renderControls();

  fireEvent.change(screen.getByLabelText('Filter by sector'), { target: { value: otherFilter } });

  expect(onFilterChange).toHaveBeenCalledWith(otherFilter);
  expect(onSortChange).not.toHaveBeenCalled();
});