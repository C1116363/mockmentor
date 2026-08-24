import { useEffect, useState } from "react";
import { isValidGithubUsername } from "../projectRules";
import { formatPrice } from "../../../utils/format";

/**
 * Asking for contributor access: GitHub username, and why you want in.
 *
 * The username is collected here rather than after payment, and that ordering is
 * the point - it is the one thing we cannot grant access without, so asking for
 * it later would mean an admin sitting on a verified payment waiting for somebody
 * to answer an email.
 */
export default function ProjectRequestModal({ project, onSubmit, onClose }) {
  const [githubUsername, setGithubUsername] = useState("");
  const [motivation, setMotivation] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const trimmed = githubUsername.trim();
  // Shown as you type, but only once there is enough to judge - flagging "r" as
  // invalid while somebody is still typing their name is just noise.
  const usernameLooksWrong = trimmed.length > 2 && !isValidGithubUsername(trimmed);

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setFieldErrors({});
    try {
      await onSubmit({ githubUsername: trimmed, motivation: motivation.trim() || null });
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true"
           aria-label={`Request access to ${project.name}`}>
        <header className="modal__head">
          <div>
            <h3>Contribute to {project.name}</h3>
            <p className="modal__sub">
              ₹{formatPrice(project.price)} · {project.accessDurationDays} days of access
            </p>
          </div>
          <button className="modal__x" onClick={onClose} aria-label="Close">×</button>
        </header>

        <ol className="pay-steps">
          <li>
            <strong>How this works</strong>
            <p className="project__note">
              This is a private repository we actually run. Once your payment is
              confirmed we add your GitHub account as a collaborator, and you raise
              pull requests like any other contributor.{" "}
              {project.leadReviewer ?? "A senior engineer"} reviews and merges them.
            </p>
          </li>

          <li>
            <strong>Your details</strong>
            <form className="form" onSubmit={submit}>
              <label className="field">
                <span>GitHub username</span>
                <div className="gh-input">
                  <span className="gh-input__at">@</span>
                  <input
                    value={githubUsername}
                    onChange={(e) => setGithubUsername(e.target.value)}
                    placeholder="your-github-handle"
                    autoComplete="off"
                    autoCapitalize="none"
                    spellCheck="false"
                    required
                  />
                </div>
                <small className="field__hint">
                  The account that gets added to the repo — check it carefully. Not
                  your email, and not your display name.
                </small>
                {usernameLooksWrong && (
                  <small className="field__error">
                    GitHub usernames are letters, numbers and single hyphens only.
                  </small>
                )}
                {fieldErrors.githubUsername && (
                  <small className="field__error">{fieldErrors.githubUsername}</small>
                )}
              </label>

              <label className="field">
                <span>Why do you want to work on this? <em>(optional)</em></span>
                <textarea
                  rows={3}
                  value={motivation}
                  onChange={(e) => setMotivation(e.target.value)}
                  placeholder="Final year student, comfortable with Spring Boot. I'd like to start with the webhook retry task."
                />
                <small className="field__hint">
                  {project.leadReviewer ?? "The reviewer"} reads this — it helps them
                  point you at the right first task.
                </small>
              </label>

              {error && <p className="notice notice--error">{error}</p>}

              <button className="btn btn--primary btn--wide" type="submit"
                      disabled={busy || !isValidGithubUsername(trimmed)}>
                {busy ? "Requesting..." : "Request access & pay"}
              </button>
            </form>
          </li>
        </ol>

        <p className="pay-note pay-note--foot">
          Nothing is charged yet — the next screen shows how to pay. Access starts
          when an admin confirms the payment.
        </p>
      </div>
    </div>
  );
}
