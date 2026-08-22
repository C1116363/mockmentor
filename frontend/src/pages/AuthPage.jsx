import { useState } from "react";
import { useAuth } from "../auth/AuthContext";

const EMPTY = { fullName: "", email: "", password: "" };

const PORTALS = [
  { key: "STUDENT", label: "Take interviews", sub: "Student / professional", canSignup: true },
  { key: "MENTOR", label: "Give interviews", sub: "Senior engineer", canSignup: true },
  { key: "ADMIN", label: "Admin", sub: "Staff login", canSignup: false },
];

const HINTS = {
  STUDENT: "For students and working professionals preparing for interviews.",
  MENTOR:
    "After signing up you'll complete a profile that an admin verifies before you can take interviews.",
  ADMIN: "Admin accounts are created internally — there is no admin signup. Log in with the account you were given.",
};

/**
 * Login and signup for all three roles.
 *
 * Admin is deliberately login-only: a public "make me an admin" endpoint would
 * let anyone grant themselves full control of the platform. Picking Admin hides
 * the signup tab entirely.
 */
export default function AuthPage() {
  const { login, signupStudent, signupMentor } = useAuth();

  const [portal, setPortal] = useState("STUDENT");
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState(EMPTY);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [wrongPortalFor, setWrongPortalFor] = useState(null);

  const current = PORTALS.find((p) => p.key === portal);
  const isSignup = mode === "signup" && current.canSignup;

  function updateField(event) {
    const { name, value } = event.target;
    setForm((c) => ({ ...c, [name]: value }));
  }

  function reset() {
    setError(null);
    setFieldErrors({});
    setWrongPortalFor(null);
  }

  function choosePortal(key) {
    setPortal(key);
    reset();
    // Admin has no signup, so fall back to login when switching to it.
    if (!PORTALS.find((p) => p.key === key).canSignup) setMode("login");
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    reset();

    try {
      if (isSignup && portal === "MENTOR") {
        await signupMentor(form);
      } else if (isSignup) {
        await signupStudent(form);
      } else {
        await login({ email: form.email, password: form.password }, portal);
      }
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
      // Point them at the right tab rather than making them work it out.
      if (err.actualRole) setWrongPortalFor(err.actualRole);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth">
      <header className="auth__intro">
        <h1>MockMentor</h1>
        <p>
          Practise a real interview with a senior engineer. Tell us what you want
          to be tested on, and we&apos;ll line up someone to take it.
        </p>

        <ul className="auth__points">
          <li>A real interview, not a chat</li>
          <li>Written feedback you keep</li>
          <li>Pick a date and a time that suit you</li>
        </ul>

        <div className="demo-box">
          <strong>Just want to look around?</strong>
          <button
            type="button"
            className="demo-box__fill"
            onClick={() => {
              choosePortal("STUDENT");
              setMode("login");
              setForm({ ...EMPTY, email: "rahul@example.com", password: "password123" });
            }}
          >
            Use the demo account
          </button>
        </div>
      </header>

      <div className="auth__card">
        <div className="portal">
          {PORTALS.map((p) => (
            <button
              key={p.key}
              type="button"
              className={`chip ${portal === p.key ? "chip--on" : ""}`}
              onClick={() => choosePortal(p.key)}
            >
              {p.label}
              <small>{p.sub}</small>
            </button>
          ))}
        </div>

        {current.canSignup ? (
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
            <button
              className={`tab ${mode === "signup" ? "tab--on" : ""}`}
              onClick={() => {
                setMode("signup");
                reset();
              }}
            >
              Sign up
            </button>
          </div>
        ) : (
          <div className="tabs">
            <button className="tab tab--on" disabled>
              Log in
            </button>
          </div>
        )}

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
              required
            />
            {fieldErrors.email && <small className="field__error">{fieldErrors.email}</small>}
          </label>

          <label className="field">
            <span>Password</span>
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={updateField}
              required
              minLength={isSignup ? 8 : undefined}
              placeholder={isSignup ? "At least 8 characters" : ""}
            />
            {fieldErrors.password && <small className="field__error">{fieldErrors.password}</small>}
          </label>

          <button className="btn btn--primary btn--wide" type="submit" disabled={busy}>
            {busy
              ? "Please wait..."
              : isSignup
              ? portal === "MENTOR"
                ? "Sign up as a mentor"
                : "Create account"
              : "Log in"}
          </button>

          {error && (
            <p className="notice notice--error">
              {error}
              {wrongPortalFor && (
                <>
                  {" "}
                  <button
                    type="button"
                    className="notice__action"
                    onClick={() => {
                      choosePortal(wrongPortalFor);
                      setError(null);
                      setWrongPortalFor(null);
                    }}
                  >
                    Switch for me
                  </button>
                </>
              )}
            </p>
          )}
        </form>
      </div>
    </div>
  );
}
