// The app's sections. Admin only appears for admins.
export const VIEWS = [
  { id: 'dashboard', label: 'Dashboard' },
  { id: 'calls', label: 'My calls' },
  { id: 'leaderboard', label: 'Leaderboard' },
  { id: 'draft', label: 'Draft room' },
  { id: 'pool', label: 'Stock pool' },
  { id: 'compare', label: 'Compare' },
];

function NavTabs({ view, isAdmin, onChange }) {
  const items = isAdmin ? [...VIEWS, { id: 'admin', label: 'Admin' }] : VIEWS;
  return (
    <nav className="nav-tabs" aria-label="Sections">
      {items.map((item) => (
        <button
          key={item.id}
          type="button"
          className={`nav-tab ${view === item.id ? 'is-active' : ''}`}
          aria-current={view === item.id ? 'page' : undefined}
          onClick={() => onChange(item.id)}
        >
          {item.label}
        </button>
      ))}
    </nav>
  );
}

export default NavTabs;
