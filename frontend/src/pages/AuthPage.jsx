import { useState } from "react";
import { useAuth } from "../auth/AuthContext";

const EMPTY = { fullName: "", email: "", password: "" };

/**
 * Login and signup for candidates - students and working professionals.
 *
 * There is no role picker any more. Anyone signing up here becomes a STUDENT.
 * Mentor and admin accounts are created and managed from the backend, not from
 * this interface.
 */
export default function AuthPage() {
  const { login, signupStudent } = useAuth();

  const [mode, setMode] = useState("login");
  const [form, setForm] = useState(EMPTY);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const isSignup = mode === "signup";

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function switchMode(next) {
    setMode(next);
    setError(null);
    setFieldErrors({});
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setFieldErrors({});

    try {
      if (isSignup) {
        await signupStudent(form);
      } else {
        await login({ email: form.email, password: form.password });
      }
      // On success AuthContext sets the user and App swaps in the dashboard.
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
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
          <li>Pick a date that suits you</li>
        </ul>

        <div className="demo-box">
          <strong>Just want to look around?</strong>
          <button
            type="button"
            className="demo-box__fill"
            onClick={() => {
              switchMode("login");
              setForm({ ...EMPTY, email: "rahul@example.com", password: "password123" });
            }}
          >
            Use the demo account
          </button>
        </div>
      </header>

      <div className="auth__card">
        <div className="tabs">
          <button className={`tab ${!isSignup ? "tab--on" : ""}`} onClick={() => switchMode("login")}>
            Log in
          </button>
          <button className={`tab ${isSignup ? "tab--on" : ""}`} onClick={() => switchMode("signup")}>
            Sign up
          </button>
        </div>

        <p className="auth__hint">
          {isSignup
            ? "For students and working professionals preparing for interviews."
            : "Welcome back. Log in to book or track your interviews."}
        </p>

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
            {busy ? "Please wait..." : isSignup ? "Create account" : "Log in"}
          </button>

          {error && <p className="notice notice--error">{error}</p>}
        </form>

        <p className="auth__switch">
          {isSignup ? (
            <>Already have an account? <button onClick={() => switchMode("login")}>Log in</button></>
          ) : (
            <>New here? <button onClick={() => switchMode("signup")}>Create an account</button></>
          )}
        </p>
      </div>
    </div>
  );
}
