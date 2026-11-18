import { formatDateTime, formatPrice, formatTradeDate } from './format.js';

test('formats a price in dollars with two decimals', () => {
  expect(formatPrice(230.5)).toBe('$230.50');
  expect(formatPrice('310.2500')).toBe('$310.25');
});

test('returns an empty string when there is no usable price', () => {
  expect(formatPrice(null)).toBe('');
  expect(formatPrice(undefined)).toBe('');
  expect(formatPrice('abc')).toBe('');
});

test('formats an ISO trade date without shifting the day', () => {
  expect(formatTradeDate('2026-11-13')).toBe('Nov 13, 2026');
  expect(formatTradeDate('2026-01-01')).toBe('Jan 1, 2026');
});

test('returns an empty string for a missing or malformed date', () => {
  expect(formatTradeDate(undefined)).toBe('');
  expect(formatTradeDate('13/11/2026')).toBe('');
});

test('formats a timestamp for the viewer', () => {
  const when = new Date(2026, 10, 17, 16, 31); // local time, so the test is zone-independent
  expect(formatDateTime(when)).toBe('Nov 17, 4:31 PM');
  expect(formatDateTime('not a date')).toBe('');
});
