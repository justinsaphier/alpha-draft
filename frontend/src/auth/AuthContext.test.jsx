import { act, render, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as stockService from '../services/stockService.js';
import { AuthProvider, useAuth } from './AuthContext.jsx';

vi.mock('../services/stockService.js', () => ({
  signIn: vi.fn(),
  signUp: vi.fn(),
}));

const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>;
const SESSION = { token: 'token-123', email: 'p@example.com', role: 'PLAYER' };

describe('AuthContext', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.clearAllMocks();
  });

  afterEach(() => {
    window.localStorage.clear();
  });

  it('starts signed out when nothing is stored', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.isSignedIn).toBe(false);
    expect(result.current.user).toBeNull();
    expect(result.current.token).toBeNull();
  });

  it('signIn stores the token and exposes the user and role', async () => {
    stockService.signIn.mockResolvedValue(SESSION);
    const { result } = renderHook(() => useAuth(), { wrapper });

    await act(() => result.current.signIn({ email: 'p@example.com', password: 'correct horse' }));

    expect(stockService.signIn).toHaveBeenCalledWith({ email: 'p@example.com', password: 'correct horse' });
    expect(result.current.isSignedIn).toBe(true);
    expect(result.current.user).toEqual({ email: 'p@example.com', role: 'PLAYER' });
    expect(result.current.token).toBe('token-123');
    expect(JSON.parse(window.localStorage.getItem('alphaDraft.session'))).toEqual(SESSION);
  });

  it('signUp stores the new session the same way', async () => {
    stockService.signUp.mockResolvedValue(SESSION);
    const { result } = renderHook(() => useAuth(), { wrapper });

    await act(() => result.current.signUp({ email: 'p@example.com', password: 'correct horse' }));

    expect(result.current.isSignedIn).toBe(true);
    expect(result.current.token).toBe('token-123');
  });

  it('a failed sign-in leaves the user signed out and rethrows the error', async () => {
    stockService.signIn.mockRejectedValue(new Error('Email or password is incorrect.'));
    const { result } = renderHook(() => useAuth(), { wrapper });

    await expect(
      act(() => result.current.signIn({ email: 'p@example.com', password: 'wrong' })),
    ).rejects.toThrow('Email or password is incorrect.');
    expect(result.current.isSignedIn).toBe(false);
    expect(window.localStorage.getItem('alphaDraft.session')).toBeNull();
  });

  it('signOut clears the user and the stored session', async () => {
    stockService.signIn.mockResolvedValue(SESSION);
    const { result } = renderHook(() => useAuth(), { wrapper });
    await act(() => result.current.signIn({ email: 'p@example.com', password: 'correct horse' }));

    act(() => result.current.signOut());

    expect(result.current.isSignedIn).toBe(false);
    expect(window.localStorage.getItem('alphaDraft.session')).toBeNull();
  });

  it('restores a saved session when the provider mounts', () => {
    window.localStorage.setItem('alphaDraft.session', JSON.stringify(SESSION));

    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.isSignedIn).toBe(true);
    expect(result.current.user.role).toBe('PLAYER');
  });

  it('ignores corrupt saved data and starts signed out', () => {
    window.localStorage.setItem('alphaDraft.session', '{not json');

    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.isSignedIn).toBe(false);
  });

  it('useAuth throws when used outside AuthProvider', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => renderHook(() => useAuth())).toThrow('useAuth must be used inside an AuthProvider');

    consoleError.mockRestore();
  });

  it('updates the rendered UI after sign-in', async () => {
    stockService.signIn.mockResolvedValue(SESSION);
    let auth;
    function Probe() {
      auth = useAuth();
      return <p>{auth.isSignedIn ? 'in' : 'out'}</p>;
    }
    const { getByText } = render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    await act(() => auth.signIn({ email: 'p@example.com', password: 'correct horse' }));

    await waitFor(() => expect(getByText('in')).toBeInTheDocument());
  });
});
