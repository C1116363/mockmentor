import { AuthProvider, useAuth } from "./auth/AuthContext";
import AuthPage from "./pages/AuthPage";
import StudentDashboard from "./pages/StudentDashboard";
import "./App.css";

/**
 * This interface is for candidates only - students and working professionals
 * who want a mock interview.
 *
 * Mentors and admins still exist in the system, but their work (assigning
 * mentors, accepting requests, managing accounts) is done through the API
 * rather than through this app.
 */
function Shell() {
  const { user, loading, logout } = useAuth();

  if (loading) {
    return <div className="booting">Loading...</div>;
  }

  if (!user) {
    return <AuthPage />;
  }

  const isCandidate = user.role === "STUDENT";

  return (
    <div className="app">
      <header className="topbar">
        <div>
          <h1>MockMentor</h1>
          <p>Book a mock interview and track it</p>
        </div>

        <div className="topbar__user">
          <div className="topbar__who">
            <strong>{user.fullName}</strong>
            <span className="topbar__email">{user.email}</span>
          </div>
          <button className="btn btn--ghost btn--sm" onClick={logout}>
            Log out
          </button>
        </div>
      </header>

      <main>
        {isCandidate ? (
          <StudentDashboard />
        ) : (
          // A mentor or admin logging in here isn't an error - there is just
          // nothing for them in this interface yet.
          <div className="panel notice-panel">
            <h2>Nothing here for you yet</h2>
            <p>
              This app is for candidates booking mock interviews. Your account is
              a <strong>{user.role.toLowerCase()}</strong> account, and that side
              of things is handled through the API for now.
            </p>
            <button className="btn btn--primary" onClick={logout}>
              Log out
            </button>
          </div>
        )}
      </main>

      <footer className="footer">MockMentor</footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <Shell />
    </AuthProvider>
  );
}
