import { useState } from "react";

/**
 * A mentor's two rates and the payroll switch.
 *
 * <h2>Both rates stay editable while payroll is off</h2>
 * You often want to set what somebody will earn before switching them on, and
 * the reverse - turning payroll off for a month - must not wipe the numbers.
 * So the switch and the rates are one form saved together, rather than the
 * switch gating the fields.
 */
export default function RateEditor({ mentor, onSave, onCancel }) {
  const [enabled, setEnabled] = useState(mentor.payrollEnabled);
  const [interviewRate, setInterviewRate] = useState(mentor.interviewRate ?? "");
  const [mentoringRate, setMentoringRate] = useState(mentor.mentoringRate ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  // The server refuses this too - this is so the button explains itself rather
  // than failing after a round trip.
  const missingRate = enabled && (interviewRate === "" || mentoringRate === "");

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await onSave({
        enabled,
        // "" means "not set", which is null on the wire - not 0, which would
        // silently mean "this mentor works for free".
        interviewRate: interviewRate === "" ? null : Number(interviewRate),
        mentoringRate: mentoringRate === "" ? null : Number(mentoringRate),
      });
    } catch {
      // onSave surfaces it; keep the form open with what was typed.
      setError("Couldn't save those rates.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="form rate-editor" onSubmit={submit}>
      <label className="toggle">
        <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
        <span className="toggle__text">
          <strong>On payroll</strong>
          <small>Turning this off keeps the rates — nothing is lost.</small>
        </span>
      </label>

      <div className="form__row">
        <label className="field">
          <span>Per mock interview</span>
          <div className="rate-input">
            <span className="rate-input__unit">₹</span>
            <input type="number" min="0" step="0.01" value={interviewRate}
                   onChange={(e) => setInterviewRate(e.target.value)} placeholder="800" />
          </div>
        </label>

        <label className="field">
          <span>Per mentoring session</span>
          <div className="rate-input">
            <span className="rate-input__unit">₹</span>
            <input type="number" min="0" step="0.01" value={mentoringRate}
                   onChange={(e) => setMentoringRate(e.target.value)} placeholder="500" />
          </div>
        </label>
      </div>

      <small className="field__hint">
        Two rates because a mock interview ends in a written scorecard and a mentoring
        session doesn&apos;t — different work, usually different money.
      </small>

      {missingRate && (
        <small className="field__error">Set both rates before turning payroll on.</small>
      )}
      {error && <p className="notice notice--error">{error}</p>}

      <div className="rate-editor__actions">
        <button className="btn btn--primary btn--sm" type="submit" disabled={busy || missingRate}>
          {busy ? "Saving..." : "Save"}
        </button>
        <button className="btn btn--ghost btn--sm" type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}
