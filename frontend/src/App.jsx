import { AuthProvider, useAuth } from "./auth/AuthContext";
import AuthPage from "./pages/AuthPage";
import StudentDashboard from "./pages/StudentDashboard";
import MentorDashboard from "./pages/MentorDashboard";
import AdminDashboard from "./pages/AdminDashboard";
import "./App.css";

const DASHBOARDS = {
  STUDENT: StudentDashboard,
  MENTOR: MentorDashboard,
  ADMIN: AdminDashboard,
};

const SUBTITLES = {
  STUDENT: "Request a mock interview and track it",
  MENTOR: "Pick up requests and take interviews",
  ADMIN: "Manage users and oversee every request",
};

/**
 * Decides what to show:
 *   still checking a saved token -> a spinner
 *   nobody logged in             -> the login / signup page
 *   logged in                    -> the dashboard for that role
 *
 * This is role-based routing without a router library. Once you add more pages,
 * swap this for react-router.
 */
function Shell() {
  const { user, loading, logout } = useAuth();

  if (loading) {
    return <div className="booting">Loading...</div>;
  }

  if (!user) {
    return <AuthPage />;
  }

  const Dashboard = DASHBOARDS[user.role];

  return (
    <div className="app">
      <header className="topbar">
        <div>
          <h1>MockMentor</h1>
          <p>{SUBTITLES[user.role]}</p>
        </div>

        <div className="topbar__user">
          <div className="topbar__who">
            <strong>{user.fullName}</strong>
            <span className={`role role--${user.role.toLowerCase()}`}>{user.role}</span>
          </div>
          <button className="btn btn--ghost btn--sm" onClick={logout}>
            Log out
          </button>
        </div>
      </header>

      <main>
        {Dashboard ? <Dashboard /> : <p className="empty">Unknown role: {user.role}</p>}
      </main>

      <footer className="footer">
        Spring Boot &middot; Spring Security + JWT &middot; MySQL &middot; React
      </footer>
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
