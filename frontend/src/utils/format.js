const CURRENCY = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const SHORT_DATE = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
const SHORT_DATE_TIME = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
});

// "230.5" or 230.5 -> "$230.50". Returns '' for anything that is not a number.
export function formatPrice(value) {
  if (value === null || value === undefined || value === '') {
    return '';
  }
  const number = Number(value);
  return Number.isFinite(number) ? CURRENCY.format(number) : '';
}

// "2026-11-13" -> "Nov 13, 2026". Built from the parts so the time zone cannot shift the day.
export function formatTradeDate(isoDate) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate ?? '');
  if (!match) {
    return '';
  }
  const [, year, month, day] = match;
  return SHORT_DATE.format(new Date(Number(year), Number(month) - 1, Number(day)));
}

// A Date (or ISO timestamp) -> "Nov 17, 4:31 PM", in the viewer's time zone.
export function formatDateTime(value) {
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? '' : SHORT_DATE_TIME.format(date);
}
