import { useEffect, useState } from "react";
import StarRating from "../../../components/StarRating";
import { useSessionFeedback } from "../useSessionFeedback";

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
 * The mentor's write-up when they close a session.
 *
 * For a mock interview it is a scorecard, and the structure is the point:
 * "4/5 on communication, weak on transactions" is far more useful to a candidate
 * than one unstructured paragraph, and it nudges the mentor to think about each
 * dimension.
 *
 * For a mentoring session the ratings and the verdict are gone entirely. They
 * are not merely optional - scoring somebody out of 5 for asking good questions
 * would make them ask fewer of them, and a "NEEDS_WORK" verdict on a
 * conversation about which stack to learn means nothing. The server nulls them
 * regardless of what a client sends; this just stops asking.
 */
export default function FeedbackModal({ request, onDone, onClose }) {
  const [form, setForm] = useState(EMPTY);
  const { error, fieldErrors, busy, submit: sendFeedback } = useSessionFeedback(request.id);

  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const set = (key) => (val) => setForm((c) => ({ ...c, [key]: val }));
  const setText = (e) => setForm((c) => ({ ...c, [e.target.name]: e.target.value }));

  // `scored` comes from the API. Defaulting to true keeps a response from
  // before this field existed behaving as a mock interview.
  const scored = request.scored !== false;

  // Only a scored session needs the rating and verdict before it can be sent.
  const ready = scored
    ? form.feedback.trim() && form.overallRating > 0 && form.recommendation
    : Boolean(form.feedback.trim());

  async function submit(event) {
    event.preventDefault();
    try {
      await sendFeedback({
        ...form,
        // The optional scores go as null rather than 0 when left untouched -
        // "not rated" and "rated zero" are different things.
        technicalRating: form.technicalRating || null,
        communicationRating: form.communicationRating || null,
        problemSolvingRating: form.problemSolvingRating || null,
      });
      onDone();
    } catch {
      // useSessionFeedback already captured the message and field errors.
    }
  }

  const counter = (field) => (
    <small className={`counter ${form[field].length > MAX * 0.9 ? "counter--warn" : ""}`}>
      {form[field].length}/{MAX}
    </small>
  );

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal modal--wide" role="dialog" aria-modal="true"
           aria-label={scored ? "Interview scorecard" : "Session notes"}>
        <header className="modal__head">
          <div>
            <h3>How did it go?</h3>
            <p className="modal__sub">
              {request.sessionLabel ?? "Mock interview"} · {request.topic} ·{" "}
              {request.student.fullName}
            </p>
          </div>
          <button className="modal__x" onClick={onClose} aria-label="Close">×</button>
        </header>

        <form className="form" onSubmit={submit}>
          {!scored && (
            <p className="pay-note">
              A mentoring session has no ratings — just write up what you covered and
              what they should do next.
            </p>
          )}

          {scored && (
          <>
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
          </>
          )}

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
              placeholder={
                scored
                  ? "Strong on annotations and the request lifecycle. Struggled once we got into transaction boundaries."
                  : "We compared backend and frontend against what they actually enjoy building, and landed on backend."
              }
              required
            />
            {counter("feedback")}
            {fieldErrors.feedback && <small className="field__error">{fieldErrors.feedback}</small>}
          </label>

          <div className="form__row">
            <label className="field">
              <span>{scored ? "What went well" : "What we agreed"}</span>
              <textarea
                name="strengths"
                rows={4}
                maxLength={MAX}
                value={form.strengths}
                onChange={setText}
                placeholder={
                  scored
                    ? "Explained their reasoning out loud without being prompted. Clean, readable code."
                    : "Start with Spring Boot, build one real API end to end, revisit in six weeks."
                }
              />
              {counter("strengths")}
            </label>

            <label className="field">
              <span>{scored ? "What to work on" : "Next steps"}</span>
              <textarea
                name="improvements"
                rows={4}
                maxLength={MAX}
                value={form.improvements}
                onChange={setText}
                placeholder={
                  scored
                    ? "Revise fetch types and the N+1 problem. Practise talking through trade-offs."
                    : "Finish the CRUD API this month, then book a mock interview to test it."
                }
              />
              {counter("improvements")}
            </label>
          </div>

          <div className="modal__actions">
            <button className="btn btn--primary" type="submit" disabled={busy || !ready}>
              {busy ? "Sending..." : scored ? "Send scorecard & close" : "Send notes & close"}
            </button>
            <button className="btn btn--ghost" type="button" onClick={onClose} disabled={busy}>
              Cancel
            </button>
          </div>

          {!ready && (
            <small className="field__hint">
              {scored
                ? "Overall rating, a verdict and a summary are needed before you can send."
                : "Write a summary before you can send."}
            </small>
          )}

          {error && <p className="notice notice--error">{error}</p>}
        </form>
      </div>
    </div>
  );
}
