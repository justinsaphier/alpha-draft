import { fmtPct, fmtPts, statusSentence } from './points.js';

test('formats points with a sign and one decimal', () => {
  expect(fmtPts(12.345)).toBe('+12.3');
  expect(fmtPts(-3)).toBe('-3.0');
  expect(fmtPts(null)).toBe('—');
});

test('formats a return as a signed percentage', () => {
  expect(fmtPct(0.1)).toBe('+10.00%');
  expect(fmtPct(-0.025)).toBe('-2.50%');
});

test('describes the league status in one sentence', () => {
  const base = { monthLabels: ['Dec 2026', 'Jan 2027', 'Feb 2027'], currentMonth: 1 };
  expect(statusSentence({ ...base, status: 'IN_SEASON' })).toBe('Jan 2027 is live.');
  expect(statusSentence({ ...base, status: 'PRESEASON' })).toBe('The draft is over. Calls for Dec 2026 are open.');
  expect(statusSentence({ ...base, status: 'COMPLETE' })).toBe('The season is complete.');
});
