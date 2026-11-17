import { useState } from 'react';
import { useAuth } from '../../auth/AuthContext.jsx';
import SignInForm from './SignInForm.jsx';
import SignUpForm from './SignUpForm.jsx';

// Shows the sign-in or sign-up form when signed out, and the account status when signed in.
function AuthPanel() {
  const { isSignedIn, user, signOut } = useAuth();
  const [mode, setMode] = useState('signIn');

  if (isSignedIn) {
    return (
      <section aria-label="Account">
        <p>
          Signed in as <strong>{user.email}</strong> ({user.role})
        </p>
        <button type="button" onClick={signOut}>
          Sign out
        </button>
      </section>
    );
  }

  return (
    <section aria-label="Account">
      <div role="group" aria-label="Account action">
        <button type="button" aria-pressed={mode === 'signIn'} onClick={() => setMode('signIn')}>
          Sign in
        </button>
        <button type="button" aria-pressed={mode === 'signUp'} onClick={() => setMode('signUp')}>
          Create account
        </button>
      </div>
      {mode === 'signIn' ? <SignInForm /> : <SignUpForm />}
    </section>
  );
}

export default AuthPanel;
