import { AuthProvider, useAuth } from "./auth/AuthContext";
import AuthPage from "./pages/AuthPage";
import StudentDashboard from "./pages/StudentDashboard";
import MentorGate from "./pages/MentorGate";
import AdminDashboard from "./pages/AdminDashboard";
import ThemeToggle from "./components/ThemeToggle";
import "./App.css";

const DASHBOARDS = {
  STUDENT: StudentDashboard,
  MENTOR: MentorGate,
  ADMIN: AdminDashboard,
};

const SUBTITLES = {
  STUDENT: "Book a mock interview and track it",
  MENTOR: "Take interviews and leave feedback",
  ADMIN: "Verify mentors and oversee the platform",
};

/** "Rahul Sharma" -> "RS" */
function initials(name) {
  return (name || "?")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0])
    .join("")
    .toUpperCase();
}

function Shell() {
  const { user, loading, logout } = useAuth();

  if (loading) return <div className="booting">Loading...</div>;
  if (!user) return <AuthPage />;

  const Dashboard = DASHBOARDS[user.role];

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar__brand">
          <div className="brand">
            <span className="brand__mark">🎯</span>
            <span className="brand__name">AbhiMentor</span>
          </div>
          <p>{SUBTITLES[user.role]}</p>
        </div>

        <div className="topbar__user">
          <div className="topbar__who">
            <strong>{user.fullName}</strong>
            <span className={`role role--${user.role.toLowerCase()}`}>{user.role}</span>
            <span className="avatar">{initials(user.fullName)}</span>
          </div>
          <ThemeToggle />
          <button className="btn btn--ghost btn--sm" onClick={logout}>
            Log out
          </button>
        </div>
      </header>

      <main>
        {Dashboard ? <Dashboard /> : <p className="empty">Unknown role: {user.role}</p>}
      </main>

      <footer className="footer">AbhiMentor · practise before it counts</footer>
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
