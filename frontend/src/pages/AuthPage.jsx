import { useState } from "react";
import { useAuth } from "../auth/AuthContext";

const DEMO_ACCOUNTS = [
  { role: "Student", email: "rahul@example.com" },
  { role: "Mentor", email: "ananya@example.com" },
  { role: "Admin", email: "admin@example.com" },
];

const EMPTY = {
  fullName: "",
  email: "",
  password: "",
  expertise: "",
  yearsOfExperience: 5,
  currentCompany: "",
  bio: "",
};

/**
 * One page that handles login and both kinds of signup.
 * `mode` is "login" or "signup"; `role` is "STUDENT" or "MENTOR".
 *
 * There is no "sign up as admin" tab on purpose - see AuthService.
 */
export default function AuthPage() {
  const { login, signupStudent, signupMentor } = useAuth();

  const [mode, setMode] = useState("login");
  const [role, setRole] = useState("STUDENT");
  const [form, setForm] = useState(EMPTY);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setFieldErrors({});

    try {
      if (mode === "login") {
        await login({ email: form.email, password: form.password });
      } else if (role === "STUDENT") {
        await signupStudent({
          fullName: form.fullName,
          email: form.email,
          password: form.password,
        });
      } else {
        await signupMentor({
          fullName: form.fullName,
          email: form.email,
          password: form.password,
          expertise: form.expertise,
          yearsOfExperience: Number(form.yearsOfExperience),
          currentCompany: form.currentCompany,
          bio: form.bio,
        });
      }
      // On success AuthContext sets the user and App swaps in the dashboard.
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    } finally {
      setBusy(false);
    }
  }

  function switchMode(nextMode) {
    setMode(nextMode);
    setError(null);
    setFieldErrors({});
  }

  const isSignup = mode === "signup";

  return (
    <div className="auth">
      <header className="auth__intro">
        <h1>MockMentor</h1>
        <p>
          Students and working professionals request mock interviews.
          Senior mentors pick them up and take the call.
        </p>

        <div className="demo-box">
          <strong>Demo accounts</strong>
          <ul>
            {DEMO_ACCOUNTS.map((account) => (
              <li key={account.email}>
                <span className="demo-box__role">{account.role}</span>
                <button
                  type="button"
                  className="demo-box__fill"
                  onClick={() => {
                    switchMode("login");
                    setForm({ ...EMPTY, email: account.email, password: "password123" });
                  }}
                >
                  {account.email}
                </button>
              </li>
            ))}
          </ul>
          <small>Password for all three: <code>password123</code></small>
        </div>
      </header>

      <div className="auth__card">
        <div className="tabs">
          <button
            className={`tab ${!isSignup ? "tab--on" : ""}`}
            onClick={() => switchMode("login")}
          >
            Log in
          </button>
          <button
            className={`tab ${isSignup ? "tab--on" : ""}`}
            onClick={() => switchMode("signup")}
          >
            Sign up
          </button>
        </div>

        {isSignup && (
          <div className="role-picker">
            <span>I am a</span>
            <div className="role-picker__options">
              <button
                type="button"
                className={`chip ${role === "STUDENT" ? "chip--on" : ""}`}
                onClick={() => setRole("STUDENT")}
              >
                Student / Professional
              </button>
              <button
                type="button"
                className={`chip ${role === "MENTOR" ? "chip--on" : ""}`}
                onClick={() => setRole("MENTOR")}
              >
                Senior Mentor
              </button>
            </div>
          </div>
        )}

        <form className="form" onSubmit={submit}>
          {isSignup && (
            <label className="field">
              <span>Full name</span>
              <input name="fullName" value={form.fullName} onChange={updateField} required />
              {fieldErrors.fullName && <small className="field__error">{fieldErrors.fullName}</small>}
            </label>
          )}

          <label className="field">
            <span>Email</span>
            <input type="email" name="email" value={form.email} onChange={updateField} required />
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
            />
            {fieldErrors.password && <small className="field__error">{fieldErrors.password}</small>}
          </label>

          {isSignup && role === "MENTOR" && (
            <>
              <label className="field">
                <span>Areas of expertise</span>
                <input
                  name="expertise"
                  value={form.expertise}
                  onChange={updateField}
                  placeholder="Java, Spring Boot, System Design"
                  required
                />
                {fieldErrors.expertise && (
                  <small className="field__error">{fieldErrors.expertise}</small>
                )}
              </label>

              <div className="form__row">
                <label className="field">
                  <span>Years of experience</span>
                  <input
                    type="number"
                    name="yearsOfExperience"
                    value={form.yearsOfExperience}
                    onChange={updateField}
                    min={3}
                    required
                  />
                  {fieldErrors.yearsOfExperience && (
                    <small className="field__error">{fieldErrors.yearsOfExperience}</small>
                  )}
                </label>

                <label className="field">
                  <span>Current company</span>
                  <input name="currentCompany" value={form.currentCompany} onChange={updateField} />
                </label>
              </div>

              <label className="field">
                <span>Short bio (optional)</span>
                <textarea name="bio" rows={2} value={form.bio} onChange={updateField} />
              </label>
            </>
          )}

          <button className="btn btn--primary btn--wide" type="submit" disabled={busy}>
            {busy ? "Please wait..." : isSignup ? "Create account" : "Log in"}
          </button>

          {error && <p className="notice notice--error">{error}</p>}
        </form>
      </div>
    </div>
  );
}
