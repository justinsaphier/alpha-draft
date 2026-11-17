import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../../auth/AuthContext.jsx';
import * as stockService from '../../services/stockService.js';
import SignUpForm from './SignUpForm.jsx';

vi.mock('../../services/stockService.js', () => ({
  signIn: vi.fn(),
  signUp: vi.fn(),
}));

function renderForm() {
  return render(
    <AuthProvider>
      <SignUpForm />
    </AuthProvider>,
  );
}

function fill(email, password) {
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: email } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: password } });
}

describe('SignUpForm', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.clearAllMocks();
  });

  it('rejects a password shorter than 8 characters without calling the backend', () => {
    renderForm();
    fill('p@example.com', 'short');

    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByRole('alert')).toHaveTextContent('Password must be at least 8 characters.');
    expect(stockService.signUp).not.toHaveBeenCalled();
  });

  it('submits the trimmed email and the password', async () => {
    stockService.signUp.mockResolvedValue({ token: 't', email: 'p@example.com', role: 'PLAYER' });
    renderForm();
    fill('  p@example.com ', 'correct horse');

    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() =>
      expect(stockService.signUp).toHaveBeenCalledWith({ email: 'p@example.com', password: 'correct horse' }),
    );
  });

  it('shows the duplicate-email message from the backend', async () => {
    const error = Object.assign(new Error('Please fix the highlighted fields.'), {
      fieldErrors: { email: 'An account with this email already exists.' },
    });
    stockService.signUp.mockRejectedValue(error);
    renderForm();
    fill('p@example.com', 'correct horse');

    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('An account with this email already exists.');
  });
});
