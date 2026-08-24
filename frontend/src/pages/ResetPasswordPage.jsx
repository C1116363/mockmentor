import { useState } from "react";
import { authApi } from "../api/authApi";
import ThemeToggle from "../components/ThemeToggle";

/**
 * The page a reset link lands on.
 *
 * <h2>How it is reached without a router</h2>
 * This app has no react-router, so there is no /reset-password route to add.
 * The link is `/?reset=<token>` and App.jsx renders this whenever that
 * parameter is present. Pulling in a router for one screen would be a
 * dependency and a restructure to replace four lines of URLSearchParams.
 *
 * <h2>The token is scrubbed from the address bar on success</h2>
 * A single-use token is spent by then, so this is belt and braces - but the URL
 * is the most-copied string on any screen, and it ends up in bookmarks, browser
 * history and pasted screenshots. Once it has done its job there is no reason
 * to leave it on display.
 */
export default function ResetPasswordPage({ token, onDone }) {
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [show, setShow] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [done, setDone] = useState(null);

  // Checked here rather than only on submit so the button is honest about
  // whether pressing it will work.
  const tooShort = password.length > 0 && password.length < 8;
  const mismatch = confirm.length > 0 && password !== confirm;
  const ready = password.length >= 8 && password === confirm;

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const result = await authApi.resetPassword(token, password);
      setDone(result.message);
      // Drop ?reset=... without reloading the page.
      window.history.replaceState({}, "", window.location.pathname);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth auth--single">
      {/* Same top bar as the login screen, so arriving from a reset link does
          not look like a different site. */}
      <header className="auth__top">
        <div className="brand">
          <span className="brand__mark">🎯</span>
          <span className="brand__name">ConfirmPlacement</span>
        </div>
        <ThemeToggle />
      </header>

      <section className="auth__card">
        {done ? (
          <div className="reset-done">
            <span className="reset-done__icon" aria-hidden="true">✓</span>
            <h3>Password changed</h3>
            <p>{done}</p>
            <button className="btn btn--primary btn--wide" onClick={onDone}>
              Log in
            </button>
          </div>
        ) : (
          <>
            <h2 className="auth__title">Choose a new password</h2>

            <form className="form" onSubmit={submit}>
              <label className="field">
                <span>New password</span>
                <div className="input-wrap">
                  <input
                    type={show ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="At least 8 characters"
                    autoComplete="new-password"
                    autoFocus
                    required
                  />
                  <button
                    type="button"
                    className="input-wrap__btn"
                    onClick={() => setShow((v) => !v)}
                    aria-label={show ? "Hide password" : "Show password"}
                  >
                    {show ? "Hide" : "Show"}
                  </button>
                </div>
                {tooShort && <small className="field__error">At least 8 characters.</small>}
              </label>

              <label className="field">
                <span>Type it again</span>
                <input
                  type={show ? "text" : "password"}
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  autoComplete="new-password"
                  required
                />
                {mismatch && <small className="field__error">These don&apos;t match.</small>}
              </label>

              <button className="btn btn--primary btn--wide" type="submit" disabled={busy || !ready}>
                {busy ? (
                  <>
                    <span className="spinner" /> Saving
                  </>
                ) : (
                  "Set new password"
                )}
              </button>

              {error && (
                <>
                  <p className="notice notice--error">{error}</p>
                  {/* A dead link is the most likely error here, and the only
                      useful next step is to ask for a fresh one - so say so
                      rather than leaving them on a form that cannot succeed. */}
                  <button type="button" className="linkish" onClick={onDone}>
                    ← Back to log in and ask for a new link
                  </button>
                </>
              )}
            </form>

            <p className="auth__footnote">
              Setting a new password signs you out everywhere else.
            </p>
          </>
        )}
      </section>
    </div>
  );
}
