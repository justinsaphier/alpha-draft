import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import StockForm from './StockForm';

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