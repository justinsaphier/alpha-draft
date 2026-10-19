import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import StockForm from './StockForm';
import { ApiError } from '../services/stockService.js';

function fillForm({ ticker, name, sector }) {
  fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: ticker } });
  fireEvent.change(screen.getByLabelText('Company name'), { target: { value: name } });
  fireEvent.change(screen.getByLabelText('Sector'), { target: { value: sector } });
}

test('renders ticker, name and sector inputs with a submit button', () => {
  // Arrange
  render(<StockForm onSubmit={() => {}} />);

  // Act — nothing to act on, this test just checks what rendered

  // Assert
  expect(screen.getByLabelText('Ticker')).toBeInTheDocument();
  expect(screen.getByLabelText('Company name')).toBeInTheDocument();
  expect(screen.getByLabelText('Sector')).toBeInTheDocument();
  expect(screen.getByText('Add Stock')).toBeInTheDocument();
});

test('calls onSubmit with the entered stock, ticker uppercased', async () => {
  // Arrange
  const handleSubmit = vi.fn().mockResolvedValue(undefined);
  render(<StockForm onSubmit={handleSubmit} />);
  fillForm({ ticker: 'msft', name: 'Microsoft Corporation', sector: 'Technology' });

  // Act
  fireEvent.click(screen.getByText('Add Stock'));

  // Assert
  await waitFor(() =>
    expect(handleSubmit).toHaveBeenCalledWith({
      ticker: 'MSFT',
      name: 'Microsoft Corporation',
      sector: 'Technology',
    }),
  );
});

test('clears the inputs after a successful add', async () => {
  // Arrange
  const handleSubmit = vi.fn().mockResolvedValue(undefined);
  render(<StockForm onSubmit={handleSubmit} />);
  fillForm({ ticker: 'KO', name: 'Coca-Cola Company', sector: 'Consumer Staples' });

  // Act
  fireEvent.click(screen.getByText('Add Stock'));

  // Assert
  await waitFor(() => expect(screen.getByLabelText('Ticker')).toHaveValue(''));
  expect(screen.getByLabelText('Company name')).toHaveValue('');
});

test('keeps the inputs when the add fails', async () => {
  // Arrange
  const handleSubmit = vi.fn().mockRejectedValue(new Error('500'));
  render(<StockForm onSubmit={handleSubmit} />);
  fillForm({ ticker: 'KO', name: 'Coca-Cola Company', sector: 'Consumer Staples' });

  // Act
  fireEvent.click(screen.getByText('Add Stock'));

  // Assert
  await waitFor(() => expect(handleSubmit).toHaveBeenCalled());
  expect(screen.getByLabelText('Ticker')).toHaveValue('KO');
});

test('shows errors and does not submit when required fields are empty', async () => {
  // Arrange
  const handleSubmit = vi.fn();
  render(<StockForm onSubmit={handleSubmit} />);

  // Act
  fireEvent.click(screen.getByText('Add Stock'));

  // Assert
  expect(await screen.findByText('Ticker is required.')).toBeInTheDocument();
  expect(screen.getByText('Company name is required.')).toBeInTheDocument();
  expect(handleSubmit).not.toHaveBeenCalled();
});

test('shows an error and does not submit an invalid ticker', async () => {
  // Arrange
  const handleSubmit = vi.fn();
  render(<StockForm onSubmit={handleSubmit} />);
  fillForm({ ticker: 'AB$C', name: 'Bad Ticker Inc', sector: 'Technology' });

  // Act
  fireEvent.click(screen.getByText('Add Stock'));

  // Assert
  expect(await screen.findByText(/only letters, numbers/)).toBeInTheDocument();
  expect(screen.getByLabelText('Ticker')).toHaveAttribute('aria-invalid', 'true');
  expect(handleSubmit).not.toHaveBeenCalled();
});

test('clears a field error once the user edits that field', async () => {
  // Arrange
  render(<StockForm onSubmit={vi.fn()} />);
  fireEvent.click(screen.getByText('Add Stock'));
  await screen.findByText('Ticker is required.');

  // Act
  fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'A' } });

  // Assert
  expect(screen.queryByText('Ticker is required.')).not.toBeInTheDocument();
  expect(screen.getByText('Company name is required.')).toBeInTheDocument();
});

test('shows backend validation errors under the matching fields', async () => {
  // Arrange
  const handleSubmit = vi
    .fn()
    .mockRejectedValue(
      new ApiError(400, 'Please fix the highlighted fields.', { ticker: 'Ticker already exists.' }),
    );
  render(<StockForm onSubmit={handleSubmit} />);
  fillForm({ ticker: 'AAPL', name: 'Apple Inc.', sector: 'Technology' });

  // Act
  fireEvent.click(screen.getByText('Add Stock'));

  // Assert
  expect(await screen.findByText('Ticker already exists.')).toBeInTheDocument();
  expect(screen.getByLabelText('Ticker')).toHaveAttribute('aria-invalid', 'true');
  expect(screen.getByLabelText('Ticker')).toHaveValue('AAPL');
});