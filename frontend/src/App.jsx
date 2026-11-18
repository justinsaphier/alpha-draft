import { useState } from 'react';
import './App.css';
import AuthPanel from './components/auth/AuthPanel.jsx';
import NavTabs from './components/NavTabs.jsx';
import { AuthProvider, useAuth } from './auth/AuthContext.jsx';
import AdminPage from './pages/AdminPage.jsx';
import CallsPage from './pages/CallsPage.jsx';
import ComparePage from './pages/ComparePage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import DraftPage from './pages/DraftPage.jsx';
import LeaderboardPage from './pages/LeaderboardPage.jsx';
import StocksPage from './pages/StocksPage.jsx';

// The signed-in shell: the header with the account, the section tabs, and the chosen page.
function Shell() {
  const { isSignedIn, isAdmin, token, signOut } = useAuth();
  const [chosenView, setView] = useState('dashboard');
  // A non-admin never sees the admin page, even if it was the last tab chosen (for example before sign-out).
  const view = chosenView === 'admin' && !isAdmin ? 'dashboard' : chosenView;

  return (
    <div className="app">
      <header className="app-header">
        <div className="brand">
          <span className="brand__mark" aria-hidden="true">α</span>
          <span className="brand__name">Alpha Draft</span>
        </div>
        <AuthPanel />
      </header>

      {isSignedIn && <NavTabs view={view} isAdmin={isAdmin} onChange={setView} />}

      {isSignedIn ? (
        <main className="page" key={token}>
          {view === 'dashboard' && <DashboardPage token={token} onUnauthorized={signOut} />}
          {view === 'calls' && <CallsPage token={token} onUnauthorized={signOut} />}
          {view === 'leaderboard' && <LeaderboardPage token={token} onUnauthorized={signOut} />}
          {view === 'draft' && <DraftPage token={token} onUnauthorized={signOut} />}
          {view === 'pool' && <StocksPage />}
          {view === 'compare' && <ComparePage token={token} onUnauthorized={signOut} />}
          {view === 'admin' && isAdmin && <AdminPage token={token} onUnauthorized={signOut} />}
        </main>
      ) : (
        <main className="page">
          <section className="welcome">
            <h1>Alpha Draft</h1>
            <p>Draft a roster of S&amp;P 500 stocks, make monthly long and short calls, and climb the league table.</p>
            <p>Sign in or create an account to join.</p>
          </section>
        </main>
      )}
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <Shell />
    </AuthProvider>
  );
}

export default App;
