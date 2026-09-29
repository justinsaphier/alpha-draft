import { render, screen, fireEvent } from '@testing-library/react';
import { vi } from 'vitest';
import StockCard from './StockCard';

const APPLE = { id: 1, ticker: 'AAPL', name: 'Apple Inc.', sector: 'Technology' };

test('renders ticker, name and sector', () => {
  // Arrange
  render(<StockCard stock={APPLE} onEdit={() => {}} onDelete={() => {}} />);

  // Act — nothing to act on, this test just checks what rendered

  // Assert
  expect(screen.getByText('AAPL')).toBeInTheDocument();
  expect(screen.getByText('Apple Inc.')).toBeInTheDocument();
  expect(screen.getByText('Technology')).toBeInTheDocument();
});

test('calls onDelete when the delete button is clicked', () => {
  // Arrange
  const handleDelete = vi.fn();
  render(<StockCard stock={APPLE} onEdit={() => {}} onDelete={handleDelete} />);

  // Act
  fireEvent.click(screen.getByText('Delete'));

  // Assert
  expect(handleDelete).toHaveBeenCalledTimes(1);
});

test('calls onEdit when the edit button is clicked', () => {
  // Arrange
  const handleEdit = vi.fn();
  render(<StockCard stock={APPLE} onEdit={handleEdit} onDelete={() => {}} />);

  // Act
  fireEvent.click(screen.getByText('Edit'));

  // Assert
  expect(handleEdit).toHaveBeenCalledTimes(1);
});