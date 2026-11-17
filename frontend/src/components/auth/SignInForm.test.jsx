import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../../auth/AuthContext.jsx';
import * as stockService from '../../services/stockService.js';
import SignInForm from './SignInForm.jsx';

vi.mock('../../services/stockService.js', () => ({
  signIn: vi.fn(),
  signUp: vi.fn(),
}));

function renderForm() {
  return render(
    <AuthProvider>
      <SignInForm />
    </AuthProvider>,
  );
}

describe('SignInForm', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders email and password fields and a submit button', () => {
    renderForm();

    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('asks for both fields without calling the backend', () => {
    renderForm();

    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(screen.getByRole('alert')).toHaveTextContent('Enter your email and password.');
    expect(stockService.signIn).not.toHaveBeenCalled();
  });

  it('submits the trimmed email and the password', async () => {
    stockService.signIn.mockResolvedValue({ token: 't', email: 'p@example.com', role: 'PLAYER' });
    renderForm();
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: '  p@example.com ' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'correct horse' } });

    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() =>
      expect(stockService.signIn).toHaveBeenCalledWith({ email: 'p@example.com', password: 'correct horse' }),
    );
  });

  it('shows the backend message when sign-in fails', async () => {
    const error = Object.assign(new Error('Email or password is incorrect.'), { fieldErrors: {} });
    stockService.signIn.mockRejectedValue(error);
    renderForm();
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'p@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong' } });

    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Email or password is incorrect.');
  });
});
