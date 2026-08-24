import { useState } from "react";
import { AuthProvider, useAuth } from "./features/auth/AuthContext";
import { SectionNavProvider, SectionTabs, useSectionNav } from "./layout/SectionNav";
import AuthPage from "./pages/AuthPage";
import ResetPasswordPage from "./pages/ResetPasswordPage";
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

/**
 * The reset token from `/?reset=<token>`, or null.
 *
 * Read once at module load rather than on every render: the value cannot change
 * without a navigation, and re-parsing per render would let the page flip back
 * to the reset screen after the token has been cleared from the URL.
 */
const RESET_TOKEN = new URLSearchParams(window.location.search).get("reset");

function Shell() {
  const { user, loading, logout } = useAuth();
  const { active } = useSectionNav();
  const [resetToken, setResetToken] = useState(RESET_TOKEN);

  if (loading) {
    return (
      <div className="booting">
        <span className="booting__mark">🎯</span>
        <span className="booting__bar"><i /></span>
      </div>
    );
  }
  /**
   * A reset link wins over everything, including an existing session.
   *
   * Checked before the logged-in branch on purpose. Somebody who asks for a
   * reset while still logged in on this browser - which is exactly what happens
   * when an account is compromised and they are racing to lock it down - would
   * otherwise land on their dashboard and never see the form.
   */
  if (resetToken) {
    return (
      <ResetPasswordPage
        token={resetToken}
        onDone={() => {
          setResetToken(null);
          // The token they just spent may have signed this session out. Clearing
          // it here means they land on the login screen rather than on a
          // dashboard whose every request is about to start failing.
          if (user) logout();
        }}
      />
    );
  }

  if (!user) return <AuthPage />;

  const Dashboard = DASHBOARDS[user.role];

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar__left">
          <div className="brand">
            <span className="brand__mark">🎯</span>
            <span className="brand__name">ConfirmPlacement</span>
          </div>
          <p className="topbar__subtitle">{SUBTITLES[user.role]}</p>
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

      {/* Every section on screen at once - see where you are, what else there
          is, and what is waiting, without opening anything. */}
      <SectionTabs />

      {/*
        `key` is doing real work: changing it makes React remount the subtree, so
        the enter animation replays on every tab change. Without it the content
        swaps instantly and the app feels like one static box being rewritten.
      */}
      <main key={active} className="view">
        {Dashboard ? <Dashboard /> : <p className="empty">Unknown role: {user.role}</p>}
      </main>

      <footer className="footer">ConfirmPlacement · practise before it counts</footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <SectionNavProvider>
        <Shell />
      </SectionNavProvider>
    </AuthProvider>
  );
}
