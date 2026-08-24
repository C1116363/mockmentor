import { useState } from "react";
import { formatPrice } from "../../plans/planRules";

const AUDIENCES = [
  { key: "ALL", label: "All students", hint: "Everyone with a student account sees it." },
  { key: "STUDENT", label: "One student", hint: "Only the student you pick. Nobody else can reach it, even with the link." },
  { key: "PLAN", label: "A plan's members", hint: "Only students whose purchase of that plan is currently active." },
];

/**
 * Send study material: upload a file, or share a link.
 *
 * The audience is a three-way choice rather than two checkboxes, because a row
 * has exactly one audience - the backend rejects being sent both a student and
 * a plan. Modelling it as radio buttons makes that impossible to get wrong from
 * this form rather than merely an error afterwards.
 */
export default function MaterialSendForm({ students, plans, onUpload, onShareLink }) {
  const [mode, setMode] = useState("FILE");
  const [audience, setAudience] = useState("ALL");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [file, setFile] = useState(null);
  const [linkUrl, setLinkUrl] = useState("");
  const [studentId, setStudentId] = useState("");
  const [planId, setPlanId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [done, setDone] = useState(null);

  function reset() {
    setTitle("");
    setDescription("");
    setFile(null);
    setLinkUrl("");
    // Deliberately keeping audience, studentId and planId: sending three files
    // to the same student in a row is the common case, and re-picking them each
    // time is the kind of friction that gets a feature abandoned.
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setFieldErrors({});
    setDone(null);

    // Exactly one of these is ever sent. The backend enforces it too.
    const target = {
      targetStudentId: audience === "STUDENT" ? studentId : null,
      targetPlanId: audience === "PLAN" ? planId : null,
    };

    try {
      const saved =
        mode === "FILE"
          ? await onUpload({ title, description, file, ...target })
          : await onShareLink({ title, description, linkUrl, ...target });

      setDone(`Sent “${saved.title}” to ${saved.audienceLabel.toLowerCase()}.`);
      reset();
      // The file input is uncontrolled, so clearing state isn't enough to make
      // the filename disappear from the widget.
      event.target.reset?.();
    } catch (err) {
      setFieldErrors(err.fieldErrors ?? {});
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const audienceReady =
    audience === "ALL" || (audience === "STUDENT" && studentId) || (audience === "PLAN" && planId);
  const contentReady = mode === "FILE" ? Boolean(file) : Boolean(linkUrl.trim());

  return (
    <form className="form send-form" onSubmit={submit}>
      <div className="chips-row">
        <button type="button" className={`chip-sm ${mode === "FILE" ? "chip-sm--on" : ""}`}
                onClick={() => setMode("FILE")}>
          Upload a file
        </button>
        <button type="button" className={`chip-sm ${mode === "LINK" ? "chip-sm--on" : ""}`}
                onClick={() => setMode("LINK")}>
          Share a link
        </button>
      </div>

      <label className="field">
        <span>Title</span>
        <input value={title} onChange={(e) => setTitle(e.target.value)} required
               placeholder="Spring Boot revision notes" />
        {fieldErrors.title && <small className="field__error">{fieldErrors.title}</small>}
      </label>

      <label className="field">
        <span>Note <em>(optional, shown under the title)</em></span>
        <textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)}
                  placeholder="Read this before session 1." />
      </label>

      {mode === "FILE" ? (
        <label className="field">
          <span>File</span>
          <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} required />
          <small className="field__hint">
            PDF, images, ZIP, or Office and text documents, up to 25 MB. HTML and SVG are
            refused — they can carry scripts.
          </small>
        </label>
      ) : (
        <label className="field">
          <span>Link</span>
          <input type="url" value={linkUrl} onChange={(e) => setLinkUrl(e.target.value)} required
                 placeholder="https://www.youtube.com/playlist?list=..." />
          <small className="field__hint">Must start with http:// or https://</small>
          {fieldErrors.linkUrl && <small className="field__error">{fieldErrors.linkUrl}</small>}
        </label>
      )}

      <fieldset className="field audience">
        <span>Who gets it?</span>
        {AUDIENCES.map((a) => (
          <label className="check" key={a.key}>
            <input type="radio" name="audience" value={a.key} checked={audience === a.key}
                   onChange={() => setAudience(a.key)} />
            <span>
              {a.label}
              <small className="field__hint"> — {a.hint}</small>
            </span>
          </label>
        ))}
      </fieldset>

      {audience === "STUDENT" && (
        <label className="field">
          <span>Which student</span>
          <select value={studentId} onChange={(e) => setStudentId(e.target.value)} required>
            <option value="">Pick a student…</option>
            {students.map((s) => (
              <option key={s.id} value={s.id}>
                {s.fullName} — {s.email}
              </option>
            ))}
          </select>
          {students.length === 0 && (
            <small className="field__hint">No student accounts yet.</small>
          )}
        </label>
      )}

      {audience === "PLAN" && (
        <label className="field">
          <span>Which plan</span>
          <select value={planId} onChange={(e) => setPlanId(e.target.value)} required>
            <option value="">Pick a plan…</option>
            {plans.filter((p) => p.active).map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} — ₹{formatPrice(p.price)}
              </option>
            ))}
          </select>
          <small className="field__hint">
            Only students whose purchase is currently active will see it — including
            anyone who buys it later.
          </small>
        </label>
      )}

      {error && <p className="notice notice--error">{error}</p>}
      {done && <p className="notice notice--success">{done}</p>}

      <button className="btn btn--primary" type="submit"
              disabled={busy || !title.trim() || !contentReady || !audienceReady}>
        {busy ? "Sending..." : mode === "FILE" ? "Upload & send" : "Share link"}
      </button>
    </form>
  );
}
