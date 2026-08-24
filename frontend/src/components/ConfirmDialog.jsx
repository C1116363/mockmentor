import { useEffect, useRef, useState } from "react";

/**
 * Confirming something, with an optional reason.
 *
 * Replaces the `window.prompt()` calls this app used for every rejection and
 * revocation. A native prompt is not just plain-looking - it cannot do any of
 * the things these particular actions need:
 *
 *  - **Say who is affected and what happens.** "Why are you rejecting this?" is
 *    a question with no context. The person clicking is about to take somebody's
 *    repository access away.
 *  - **Warn that the text is not private.** These reasons are shown to the
 *    student. Someone typing "didn't like the vibe" into a native prompt has no
 *    idea it will be read by the person it is about.
 *  - **Reject a useless reason.** A prompt accepts "x" and hands it to somebody
 *    as their explanation.
 *  - **Show the server's error where it happened.** A failed call after a prompt
 *    has nowhere to go but a second alert.
 *
 * @param reason omit entirely for a plain yes/no confirm.
 */
export default function ConfirmDialog({
  title,
  children,
  intent = "danger",
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  reason,
  onConfirm,
  onCancel,
}) {
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [touched, setTouched] = useState(false);
  const inputRef = useRef(null);

  const minLength = reason?.minLength ?? 10;
  const trimmed = text.trim();
  const tooShort = reason && trimmed.length < minLength;

  useEffect(() => {
    // Focus the field, or the dialog itself for a plain confirm, so the keyboard
    // works without reaching for the mouse first.
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === "Escape" && !busy) onCancel();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onCancel, busy]);

  async function confirm() {
    if (tooShort) {
      setTouched(true);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onConfirm(reason ? trimmed : undefined);
    } catch (e) {
      // Stays open with the error attached, so the typed reason is not lost and
      // the person can correct and retry.
      setError(e.message);
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop"
         onMouseDown={(e) => e.target === e.currentTarget && !busy && onCancel()}>
      <div className={`modal modal--confirm modal--${intent}`} role="alertdialog"
           aria-modal="true" aria-label={title}>
        <header className="modal__head">
          <div>
            <h3>{title}</h3>
          </div>
          <button className="modal__x" onClick={onCancel} disabled={busy} aria-label="Close">
            ×
          </button>
        </header>

        {children && <div className="confirm__body">{children}</div>}

        {reason && (
          <label className="field">
            <span>{reason.label ?? "Reason"}</span>
            <textarea
              ref={inputRef}
              rows={3}
              value={text}
              onChange={(e) => setText(e.target.value)}
              onBlur={() => setTouched(true)}
              placeholder={reason.placeholder}
              disabled={busy}
              // Ctrl/Cmd+Enter submits - the shortcut anyone who types in
              // textareas already expects.
              onKeyDown={(e) => {
                if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) confirm();
              }}
            />
            {reason.hint && (
              <small className="field__hint confirm__seen">👁 {reason.hint}</small>
            )}
            {touched && tooShort && (
              <small className="field__error">
                {trimmed.length === 0
                  ? "A reason is required."
                  : `A bit more detail — at least ${minLength} characters. They only get this to go on.`}
              </small>
            )}
          </label>
        )}

        {error && <p className="notice notice--error">{error}</p>}

        <div className="modal__actions">
          <button
            className={`btn ${intent === "danger" ? "btn--danger" : "btn--primary"}`}
            onClick={confirm}
            disabled={busy || (reason && trimmed.length === 0)}
          >
            {busy ? "Working..." : confirmLabel}
          </button>
          <button className="btn btn--ghost" onClick={onCancel} disabled={busy}>
            {cancelLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
