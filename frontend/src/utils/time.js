// "1:05" for 65 seconds, "0:00" for anything at or below zero.
export function formatCountdown(milliseconds) {
  const total = Math.max(0, Math.ceil(milliseconds / 1000));
  const minutes = Math.floor(total / 60);
  const seconds = String(total % 60).padStart(2, '0');
  return `${minutes}:${seconds}`;
}
