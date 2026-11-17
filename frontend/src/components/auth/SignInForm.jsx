import { useState } from 'react';
import { useAuth } from '../../auth/AuthContext.jsx';

// Shows the backend's message, or the first field error the backend sent.
function errorMessage(err) {
  const fieldMessage = Object.values(err?.fieldErrors ?? {})[0];
  return fieldMessage || err?.message || 'Could not sign in. Please try again.';
}

function SignInForm() {
  const { signIn } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError('Enter your email and password.');
      return;
    }

    setError('');
    setIsSubmitting(true);
    try {
      await signIn({ email: email.trim(), password });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} aria-label="Sign in">
      <label>
        Email
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
      </label>
      <label>
        Password
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
        />
      </label>
      {error && <p role="alert" className="error">{error}</p>}
      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Signing in…' : 'Sign in'}
      </button>
    </form>
  );
}

export default SignInForm;
