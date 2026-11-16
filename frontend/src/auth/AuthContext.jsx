import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import * as stockService from '../services/stockService.js';

const STORAGE_KEY = 'alphaDraft.session';

const AuthContext = createContext(null);

// Reads the saved session. Storage can be unavailable or hold bad data, so any failure means "signed out".
function readStoredSession() {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    const session = raw ? JSON.parse(raw) : null;
    return session && typeof session.token === 'string' ? session : null;
  } catch {
    return null;
  }
}

function writeStoredSession(session) {
  try {
    if (session) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } else {
      window.localStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // Storage is unavailable (e.g. private mode); the session still works until reload.
  }
}

// Holds the signed-in user, their role and token. Components read it with useAuth().
export function AuthProvider({ children }) {
  const [session, setSession] = useState(readStoredSession);

  const storeSession = useCallback((result) => {
    const next = { token: result.token, email: result.email, role: result.role };
    writeStoredSession(next);
    setSession(next);
    return next;
  }, []);

  const signIn = useCallback(
    async (credentials) => storeSession(await stockService.signIn(credentials)),
    [storeSession],
  );

  const signUp = useCallback(
    async (credentials) => storeSession(await stockService.signUp(credentials)),
    [storeSession],
  );

  const signOut = useCallback(() => {
    writeStoredSession(null);
    setSession(null);
  }, []);

  const value = useMemo(
    () => ({
      isSignedIn: session !== null,
      user: session ? { email: session.email, role: session.role } : null,
      token: session ? session.token : null,
      signIn,
      signUp,
      signOut,
    }),
    [session, signIn, signUp, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider');
  }
  return context;
}
