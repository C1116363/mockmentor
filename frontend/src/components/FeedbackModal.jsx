import { useEffect, useState } from "react";
import { api } from "../api/client";
import StarRating from "./StarRating";

const VERDICTS = [
  {
    key: "READY",
    label: "Ready",
    blurb: "Would pass a real round today",
    icon: "✓",
  },
  {
    key: "ALMOST_READY",
    label: "Almost there",
    blurb: "A few gaps to close first",
    icon: "◐",
  },
  {
    key: "NEEDS_WORK",
    label: "Needs work",
    blurb: "Significant prep still needed",
    icon: "○",
  },
];

const EMPTY = {
  feedback: "",
  strengths: "",
  improvements: "",
  overallRating: 0,
  technicalRating: 0,
  communicationRating: 0,
  problemSolvingRating: 0,
  recommendation: "",
};

const MAX = 2000;

/**
 * The mentor's scorecard.
 *
 * Replaces a window.prompt(). Beyond looking better, the structure is the
 * point: "4/5 on communication, weak on transactions" is far more useful to a
 * candidate than one unstructured paragraph, and it nudges the mentor to
 * actually think about each dimension.
 */
export default function FeedbackModal({ request, onDone, onClose }) {
  const [form, setForm] = useState(EMPTY);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const set = (key) => (val) => setForm((c) => ({ ...c, [key]: val }));
  const setText = (e) => setForm((c) => ({ ...c, [e.target.name]: e.target.value }));

  const ready = form.feedback.trim() && form.overallRating > 0 && form.recommendation;

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setFieldErrors({});

    try {
      await api.completeRequest(request.id, {
        ...form,
        // The optional scores go as null rather than 0 when left untouched -
        // "not rated" and "rated zero" are different things.
        technicalRating: form.technicalRating || null,
        communicationRating: form.communicationRating || null,
        problemSolvingRating: form.problemSolvingRating || null,
      });
      onDone();
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    } finally {
      setBusy(false);
    }
  }

  const counter = (field) => (
    <small className={`counter ${form[field].length > MAX * 0.9 ? "counter--warn" : ""}`}>
      {form[field].length}/{MAX}
    </small>
  );

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal modal--wide" role="dialog" aria-modal="true" aria-label="Interview feedback">
        <header className="modal__head">
          <div>
            <h3>How did it go?</h3>
            <p className="modal__sub">
              {request.topic} · {request.student.fullName}
            </p>
          </div>
          <button className="modal__x" onClick={onClose} aria-label="Close">×</button>
        </header>

        <form className="form" onSubmit={submit}>
          <StarRating
            name="overallRating"
            label="Overall"
            hint="Required"
            value={form.overallRating}
            onChange={set("overallRating")}
          />
          {fieldErrors.overallRating && (
            <small className="field__error">{fieldErrors.overallRating}</small>
          )}

          <div className="rating-grid">
            <StarRating
              name="technicalRating"
              label="Technical knowledge"
              value={form.technicalRating}
              onChange={set("technicalRating")}
            />
            <StarRating
              name="problemSolvingRating"
              label="Problem solving"
              value={form.problemSolvingRating}
              onChange={set("problemSolvingRating")}
            />
            <StarRating
              name="communicationRating"
              label="Communication"
              value={form.communicationRating}
              onChange={set("communicationRating")}
            />
          </div>

          <div className="field">
            <span>Verdict</span>
            <div className="verdicts">
              {VERDICTS.map((v) => (
                <button
                  key={v.key}
                  type="button"
                  className={`verdict verdict--${v.key.toLowerCase()} ${
                    form.recommendation === v.key ? "verdict--on" : ""
                  }`}
                  onClick={() => set("recommendation")(v.key)}
                  aria-pressed={form.recommendation === v.key}
                >
                  <span className="verdict__icon">{v.icon}</span>
                  <strong>{v.label}</strong>
                  <small>{v.blurb}</small>
                </button>
              ))}
            </div>
            {fieldErrors.recommendation && (
              <small className="field__error">{fieldErrors.recommendation}</small>
            )}
          </div>

          <label className="field">
            <span>
              Summary <em>— the first thing they&apos;ll read</em>
            </span>
            <textarea
              name="feedback"
              rows={3}
              maxLength={MAX}
              value={form.feedback}
              onChange={setText}
              placeholder="Strong on annotations and the request lifecycle. Struggled once we got into transaction boundaries."
              required
            />
            {counter("feedback")}
            {fieldErrors.feedback && <small className="field__error">{fieldErrors.feedback}</small>}
          </label>

          <div className="form__row">
            <label className="field">
              <span>What went well</span>
              <textarea
                name="strengths"
                rows={4}
                maxLength={MAX}
                value={form.strengths}
                onChange={setText}
                placeholder="Explained their reasoning out loud without being prompted. Clean, readable code."
              />
              {counter("strengths")}
            </label>

            <label className="field">
              <span>What to work on</span>
              <textarea
                name="improvements"
                rows={4}
                maxLength={MAX}
                value={form.improvements}
                onChange={setText}
                placeholder="Revise fetch types and the N+1 problem. Practise talking through trade-offs."
              />
              {counter("improvements")}
            </label>
          </div>

          <div className="modal__actions">
            <button className="btn btn--primary" type="submit" disabled={busy || !ready}>
              {busy ? "Sending..." : "Send feedback & close interview"}
            </button>
            <button className="btn btn--ghost" type="button" onClick={onClose} disabled={busy}>
              Cancel
            </button>
          </div>

          {!ready && (
            <small className="field__hint">
              Overall rating, a verdict and a summary are needed before you can send.
            </small>
          )}

          {error && <p className="notice notice--error">{error}</p>}
        </form>
      </div>
    </div>
  );
}
