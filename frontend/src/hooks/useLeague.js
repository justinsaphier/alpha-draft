import { useCallback, useEffect, useRef, useState } from 'react';
import { getLeague, getStandings } from '../services/leagueService.js';

// The league's summary (status, your team, the teams) and the standings. Pages call refresh()
// after anything that changes the league, so the screen never shows stale state.
function useLeague(token, onUnauthorized) {
  const [summary, setSummary] = useState(null);
  const [standings, setStandings] = useState([]);
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const onUnauthorizedRef = useRef(onUnauthorized);
  useEffect(() => {
    onUnauthorizedRef.current = onUnauthorized;
  });

  const refresh = useCallback(async () => {
    try {
      const [nextSummary, nextStandings] = await Promise.all([getLeague(token), getStandings(token)]);
      setSummary(nextSummary);
      setStandings(nextStandings ?? []);
      setError(null);
      return nextSummary;
    } catch (err) {
      if (err?.status === 401 && onUnauthorizedRef.current) {
        onUnauthorizedRef.current();
      }
      setError(err.message);
      return null;
    } finally {
      setIsLoading(false);
    }
  }, [token]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return { summary, standings, error, isLoading, refresh };
}

export default useLeague;
