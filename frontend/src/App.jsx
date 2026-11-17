import './App.css';
import AuthPanel from './components/auth/AuthPanel.jsx';
import { AuthProvider } from './auth/AuthContext.jsx';
import StocksPage from './pages/StocksPage.jsx';

function App() {
  return (
    <AuthProvider>
      <AuthPanel />
      <StocksPage />
    </AuthProvider>
  );
}

export default App;
