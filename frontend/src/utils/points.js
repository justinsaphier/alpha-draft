// "+12.5" / "-3.0" for points, "+4.20%" for returns.
export function fmtPts(value, digits = 1) {
  if (value == null || Number.isNaN(Number(value))) {
    return '—';
  }
  const n = Number(value);
  return `${n >= 0 ? '+' : ''}${n.toFixed(digits)}`;
}

export function fmtPct(value, digits = 2) {
  const n = Number(value);
  return `${n >= 0 ? '+' : ''}${(n * 100).toFixed(digits)}%`;
}

// The league status as one plain sentence for the header.
export function statusSentence(summary) {
  if (!summary) {
    return '';
  }
  const label = summary.monthLabels?.[summary.currentMonth] ?? '';
  switch (summary.status) {
    case 'REGISTRATION':
      return summary.draftAt
        ? `Sign-ups are open. The draft starts ${new Date(summary.draftAt).toLocaleString()}.`
        : 'Sign-ups are open. The admin will set the draft day.';
    case 'DRAFTING':
      return 'The snake draft is in progress.';
    case 'PRESEASON':
      return `The draft is over. Calls for ${summary.monthLabels?.[0]} are open.`;
    case 'IN_SEASON':
      return `${label} is live.`;
    case 'COMPLETE':
      return 'The season is complete.';
    default:
      return '';
  }
}
