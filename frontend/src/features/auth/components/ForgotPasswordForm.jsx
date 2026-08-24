import { useState } from "react";
import { authApi } from "../../../api/authApi";

/**
 * "Send me a reset link."
 *
 * <h2>Why success looks like a dead end</h2>
 * On success this stops asking and shows the server's message, with no way to
 * resubmit except an explicit "try a different address". That is on purpose:
 * the server answers identically whether or not the account exists, so leaving
 * an inviting Send button under the confirmation would coax people into
 * hammering it looking for a different answer - and there isn't one. The rate
 * limit would then quietly eat the extra requests, which is worse than a
 * screen that simply says it is done.
 */
export default function ForgotPasswordForm({ onBack }) {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const result = await authApi.forgotPassword(email.trim());
      setSent(result.message);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  if (sent) {
    return (
      <div className="reset-done">
        <span className="reset-done__icon" aria-hidden="true">📬</span>
        <h3>Check your email</h3>
        <p>{sent}</p>
        <div className="reset-done__actions">
          <button type="button" className="btn btn--primary btn--wide" onClick={onBack}>
            Back to log in
          </button>
          <button
            type="button"
            className="btn btn--ghost btn--wide"
            onClick={() => {
              setSent(null);
              setEmail("");
            }}
          >
            Try a different address
          </button>
        </div>
      </div>
    );
  }

  return (
    <form className="form" onSubmit={submit}>
      <p className="auth__hint">
        Enter the email you signed up with and we&apos;ll send you a link to choose a
        new password.
      </p>

      <label className="field">
        <span>Email</span>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="you@example.com"
          autoComplete="email"
          autoFocus
          required
        />
      </label>

      <button className="btn btn--primary btn--wide" type="submit" disabled={busy || !email.trim()}>
        {busy ? (
          <>
            <span className="spinner" /> Sending
          </>
        ) : (
          "Send me a reset link"
        )}
      </button>

      {error && <p className="notice notice--error">{error}</p>}

      <button type="button" className="linkish" onClick={onBack}>
        ← Back to log in
      </button>
    </form>
  );
}
