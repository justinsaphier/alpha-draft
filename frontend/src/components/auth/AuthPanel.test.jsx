import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../../auth/AuthContext.jsx';
import * as stockService from '../../services/stockService.js';
import AuthPanel from './AuthPanel.jsx';

vi.mock('../../services/stockService.js', () => ({
  signIn: vi.fn(),
  signUp: vi.fn(),
}));

function renderPanel() {
  return render(
    <AuthProvider>
      <AuthPanel />
    </AuthProvider>,
  );
}

describe('AuthPanel', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.clearAllMocks();
  });

  it('shows the sign-in form first and can switch to create account', () => {
    renderPanel();

    expect(screen.getByRole('form', { name: 'Sign in' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByRole('form', { name: 'Create account' })).toBeInTheDocument();
  });

  it('after signing in, shows the account and a sign-out button that clears it', async () => {
    stockService.signIn.mockResolvedValue({ token: 't', email: 'p@example.com', role: 'PLAYER' });
    renderPanel();
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'p@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'correct horse' } });
    const signInForm = screen.getByRole('form', { name: 'Sign in' });
    fireEvent.click(within(signInForm).getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('p@example.com')).toBeInTheDocument();
    expect(screen.getByText(/\(PLAYER\)/)).toBeInTheDocument();
    expect(screen.queryByRole('form', { name: 'Sign in' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }));

    await waitFor(() => expect(screen.getByRole('form', { name: 'Sign in' })).toBeInTheDocument());
  });
});
