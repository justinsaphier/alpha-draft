import { fireEvent, render, screen } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import PriceRefreshPanel from './PriceRefreshPanel.jsx';

const IDLE = { configured: true, running: false, lastFinishedAt: null, lastStored: null, lastError: null };

test('explains that no prices are stored yet and offers a refresh', () => {
  const onRefresh = vi.fn();
  render(<PriceRefreshPanel status={IDLE} onRefresh={onRefresh} />);

  expect(screen.getByText('No closing prices stored yet.')).toBeInTheDocument();
  fireEvent.click(screen.getByText('Refresh closing prices'));
  expect(onRefresh).toHaveBeenCalledTimes(1);
});

test('shows when the last update finished and how many closes it stored', () => {
  render(
    <PriceRefreshPanel
      status={{ ...IDLE, lastFinishedAt: '2026-11-17T21:31:00Z', lastStored: 503 }}
      onRefresh={() => {}}
    />,
  );

  expect(screen.getByText(/Last update finished/)).toBeInTheDocument();
  expect(screen.getByText(/storing 503 new closes/)).toBeInTheDocument();
});

test('disables the button and says so while an update runs', () => {
  render(<PriceRefreshPanel status={{ ...IDLE, running: true }} onRefresh={() => {}} />);

  expect(screen.getByRole('button', { name: 'Updating…' })).toBeDisabled();
  expect(screen.getByText(/this page updates by itself/)).toBeInTheDocument();
});

test('explains the missing API key instead of offering a refresh that cannot run', () => {
  render(<PriceRefreshPanel status={{ ...IDLE, configured: false }} onRefresh={() => {}} />);

  expect(screen.getByText(/TWELVE_DATA_API_KEY/)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Refresh closing prices' })).toBeDisabled();
});

test('shows the error from the last failed update and any start error', () => {
  render(
    <PriceRefreshPanel
      status={{ ...IDLE, lastError: 'database unavailable' }}
      error="A price update is already running."
      onRefresh={() => {}}
    />,
  );

  expect(screen.getByText(/database unavailable/)).toBeInTheDocument();
  expect(screen.getByText('A price update is already running.')).toBeInTheDocument();
});
