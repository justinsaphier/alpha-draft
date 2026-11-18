import { formatCountdown } from './time.js';

test('counts down as minutes and seconds', () => {
  expect(formatCountdown(65_000)).toBe('1:05');
  expect(formatCountdown(9_400)).toBe('0:10');
  expect(formatCountdown(0)).toBe('0:00');
  expect(formatCountdown(-5_000)).toBe('0:00');
});
