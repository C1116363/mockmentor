import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import ThemeToggle from "../components/ThemeToggle";

const EMPTY = { fullName: "", email: "", password: "" };

const PORTALS = [
  {
    key: "STUDENT",
    label: "Take interviews",
    sub: "Student / professional",
    icon: "🎯",
    canSignup: true,
  },
  {
    key: "MENTOR",
    label: "Give interviews",
    sub: "Senior engineer",
    icon: "🧭",
    canSignup: true,
  },
  { key: "ADMIN", label: "Admin", sub: "Staff login", icon: "🛡️", canSignup: false },
];

const HINTS = {
  STUDENT: "For students and working professionals preparing for interviews.",
  MENTOR:
    "After signing up you'll complete a profile that an admin verifies before you can take interviews.",
  ADMIN: "Admin accounts are created internally — log in with the account you were given.",
};

const POINTS = [
  { icon: "🎙️", title: "A real interview", text: "Not a chat. One honest hour with a senior engineer." },
  { icon: "📋", title: "A written scorecard", text: "Ratings per skill, and exactly what to fix." },
  { icon: "📅", title: "Your slot, your call", text: "Pick any free hour in the next 30 days." },
];

export default function AuthPage() {
  const { login, signupStudent, signupMentor } = useAuth();

  const [portal, setPortal] = useState("STUDENT");
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState(EMPTY);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const current = PORTALS.find((p) => p.key === portal);
  const isSignup = mode === "signup" && current.canSignup;

  const updateField = (e) =>
    setForm((c) => ({ ...c, [e.target.name]: e.target.value }));

  function reset() {
    setError(null);
    setFieldErrors({});
  }

  function choosePortal(key) {
    setPortal(key);
    reset();
    if (!PORTALS.find((p) => p.key === key).canSignup) setMode("login");
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    reset();
    try {
      if (isSignup && portal === "MENTOR") await signupMentor(form);
      else if (isSignup) await signupStudent(form);
      else await login({ email: form.email, password: form.password }, portal);
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth">
      <div className="auth__theme">
        <ThemeToggle />
      </div>

      {/* left: the pitch */}
      <section className="auth__intro">
        <div className="brand">
          <span className="brand__mark">🎯</span>
          <span className="brand__name">AbhiMentor</span>
        </div>

        <h1>
          Walk in <span className="grad">already prepared</span>
        </h1>
        <p className="auth__lede">
          Practise with engineers who take real interviews for a living, then get
          a written breakdown of exactly what to fix.
        </p>

        <ul className="auth__points">
          {POINTS.map((p) => (
            <li key={p.title}>
              <span className="auth__point-icon">{p.icon}</span>
              <span>
                <strong>{p.title}</strong>
                <small>{p.text}</small>
              </span>
            </li>
          ))}
        </ul>
      </section>

      {/* right: the form */}
      <section className="auth__card">
        <div className="portal">
          {PORTALS.map((p) => (
            <button
              key={p.key}
              type="button"
              className={`portal__opt ${portal === p.key ? "portal__opt--on" : ""}`}
              onClick={() => choosePortal(p.key)}
              aria-pressed={portal === p.key}
            >
              <span className="portal__icon">{p.icon}</span>
              <strong>{p.label}</strong>
              <small>{p.sub}</small>
            </button>
          ))}
        </div>

        <div className="tabs">
          <button
            className={`tab ${mode === "login" ? "tab--on" : ""}`}
            onClick={() => {
              setMode("login");
              reset();
            }}
          >
            Log in
          </button>
          {current.canSignup ? (
            <button
              className={`tab ${mode === "signup" ? "tab--on" : ""}`}
              onClick={() => {
                setMode("signup");
                reset();
              }}
            >
              Sign up
            </button>
          ) : (
            <button className="tab tab--off" disabled title="Admin accounts are created internally">
              Sign up
            </button>
          )}
        </div>

        <p className="auth__hint">{HINTS[portal]}</p>

        <form className="form" onSubmit={submit}>
          {isSignup && (
            <label className="field">
              <span>Full name</span>
              <input
                name="fullName"
                value={form.fullName}
                onChange={updateField}
                placeholder="Rahul Sharma"
                autoComplete="name"
                required
              />
              {fieldErrors.fullName && <small className="field__error">{fieldErrors.fullName}</small>}
            </label>
          )}

          <label className="field">
            <span>Email</span>
            <input
              type="email"
              name="email"
              value={form.email}
              onChange={updateField}
              placeholder="you@example.com"
              autoComplete="email"
              required
            />
            {fieldErrors.email && <small className="field__error">{fieldErrors.email}</small>}
          </label>

          <label className="field">
            <span>Password</span>
            <div className="input-wrap">
              <input
                type={showPassword ? "text" : "password"}
                name="password"
                value={form.password}
                onChange={updateField}
                required
                minLength={isSignup ? 8 : undefined}
                placeholder={isSignup ? "At least 8 characters" : "••••••••"}
                autoComplete={isSignup ? "new-password" : "current-password"}
              />
              <button
                type="button"
                className="input-wrap__btn"
                onClick={() => setShowPassword((v) => !v)}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? "Hide" : "Show"}
              </button>
            </div>
            {fieldErrors.password && <small className="field__error">{fieldErrors.password}</small>}
          </label>

          <button className="btn btn--primary btn--wide" type="submit" disabled={busy}>
            {busy ? (
              <>
                <span className="spinner" /> Please wait
              </>
            ) : isSignup ? (
              portal === "MENTOR" ? "Sign up as a mentor" : "Create account"
            ) : (
              "Log in"
            )}
          </button>

          {error && <p className="notice notice--error">{error}</p>}

          {!isSignup && (
            <p className="auth__footnote">
              Sure your password is right? Check you&apos;ve picked the correct
              option at the top.
            </p>
          )}
        </form>
      </section>
    </div>
  );
}
