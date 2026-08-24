import { useEffect, useState } from "react";

const DIFFICULTIES = [
  { key: "BEGINNER", label: "Beginner", hint: "Good first project" },
  { key: "INTERMEDIATE", label: "Intermediate", hint: "Some experience needed" },
  { key: "ADVANCED", label: "Advanced", hint: "For strong contributors" },
];

const BLANK = {
  name: "",
  summary: "",
  description: "",
  techStack: "",
  sampleTasks: "",
  repoOwner: "",
  repoName: "",
  onboardingUrl: "",
  price: "",
  accessDurationDays: 90,
  maxContributors: "",
  difficulty: "INTERMEDIATE",
  leadReviewerId: "",
  displayOrder: 0,
  active: true,
};

/**
 * Create or edit a live project.
 *
 * The repo is two fields, owner and name, rather than one URL - that is what the
 * GitHub API takes, and asking for a URL just means parsing it back apart and
 * getting a trailing slash wrong somewhere.
 */
export default function ProjectEditor({ project, reviewers, onSave, onCancel }) {
  const [form, setForm] = useState(BLANK);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!project) {
      setForm(BLANK);
      return;
    }
    setForm({
      name: project.name ?? "",
      summary: project.summary ?? "",
      description: project.description ?? "",
      // The API hands these back parsed; the editor wants the raw text.
      techStack: (project.techStack ?? []).join(", "),
      sampleTasks: (project.sampleTasks ?? []).join("\n"),
      repoOwner: project.repoFullName?.split("/")[0] ?? "",
      repoName: project.repoFullName?.split("/")[1] ?? "",
      onboardingUrl: project.onboardingUrl ?? "",
      price: String(project.price ?? ""),
      accessDurationDays: project.accessDurationDays ?? 90,
      maxContributors: project.maxContributors ?? "",
      difficulty: project.difficulty ?? "INTERMEDIATE",
      leadReviewerId: "",
      displayOrder: project.displayOrder ?? 0,
      active: project.active ?? true,
    });
  }, [project]);

  const update = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((c) => ({ ...c, [name]: type === "checkbox" ? checked : value }));
  };

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setFieldErrors({});
    try {
      await onSave({
        ...form,
        price: Number(form.price),
        accessDurationDays: Number(form.accessDurationDays),
        // Empty means "no limit", which the API expects as null rather than 0.
        maxContributors: form.maxContributors === "" ? null : Number(form.maxContributors),
        leadReviewerId: form.leadReviewerId === "" ? null : Number(form.leadReviewerId),
        displayOrder: Number(form.displayOrder),
      });
    } catch (err) {
      setFieldErrors(err.fieldErrors ?? {});
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onCancel()}>
      <div className="modal modal--wide" role="dialog" aria-modal="true">
        <header className="modal__head">
          <div>
            <h3>{project ? `Edit ${project.name}` : "New live project"}</h3>
            <p className="modal__sub">
              {project
                ? "Every field is replaced with what you send."
                : "Point it at a repository you actually own."}
            </p>
          </div>
          <button className="modal__x" onClick={onCancel} aria-label="Close">×</button>
        </header>

        <form className="form" onSubmit={submit}>
          <label className="field">
            <span>Name</span>
            <input name="name" value={form.name} onChange={update} required
                   placeholder="eSign gateway" />
            {fieldErrors.name && <small className="field__error">{fieldErrors.name}</small>}
          </label>

          <label className="field">
            <span>Summary <em>(one line on the card)</em></span>
            <input name="summary" value={form.summary} onChange={update}
                   placeholder="Production signing flows, used by real customers" />
          </label>

          <div className="form__row">
            <label className="field">
              <span>GitHub owner</span>
              <input name="repoOwner" value={form.repoOwner} onChange={update} required
                     placeholder="your-org" autoCapitalize="none" spellCheck="false" />
              {fieldErrors.repoOwner && (
                <small className="field__error">{fieldErrors.repoOwner}</small>
              )}
            </label>

            <label className="field">
              <span>Repository name</span>
              <input name="repoName" value={form.repoName} onChange={update} required
                     placeholder="esign-gateway" autoCapitalize="none" spellCheck="false" />
              {fieldErrors.repoName && (
                <small className="field__error">{fieldErrors.repoName}</small>
              )}
            </label>
          </div>

          <p className="pay-note">
            Nothing here checks that <span className="mono">
              {form.repoOwner || "owner"}/{form.repoName || "repo"}
            </span> exists — a typo looks fine until somebody pays and the invite fails.
            Paste it from the GitHub URL.
          </p>

          <label className="field">
            <span>Description</span>
            <textarea name="description" rows={3} value={form.description} onChange={update}
                      placeholder="What the service does, and what a newcomer would start on." />
          </label>

          <label className="field">
            <span>Tech stack <em>(comma separated)</em></span>
            <input name="techStack" value={form.techStack} onChange={update}
                   placeholder="Java, Spring Boot, MySQL, Docker" />
          </label>

          <label className="field">
            <span>Sample tasks <em>(one per line)</em></span>
            <textarea name="sampleTasks" rows={4} value={form.sampleTasks} onChange={update}
                      placeholder={"Add retry handling to the webhook dispatcher\nWrite tests for the PDF signing path"} />
            <small className="field__hint">
              Concrete tasks do more to attract the right contributor than a
              description does.
            </small>
          </label>

          <div className="form__row">
            <label className="field">
              <span>Price (₹)</span>
              <input name="price" type="number" min="0" step="1" value={form.price}
                     onChange={update} required placeholder="7999" />
              {fieldErrors.price && <small className="field__error">{fieldErrors.price}</small>}
            </label>

            <label className="field">
              <span>Access (days)</span>
              <input name="accessDurationDays" type="number" min="1" max="3650"
                     value={form.accessDurationDays} onChange={update} required />
            </label>
          </div>

          <div className="form__row">
            <label className="field">
              <span>Max contributors <em>(blank = no limit)</em></span>
              <input name="maxContributors" type="number" min="1" max="500"
                     value={form.maxContributors} onChange={update} placeholder="4" />
              <small className="field__hint">
                One reviewer cannot meaningfully review thirty newcomers at once.
              </small>
            </label>

            <label className="field">
              <span>Lead reviewer</span>
              <select name="leadReviewerId" value={form.leadReviewerId} onChange={update}>
                <option value="">
                  {project?.leadReviewer ? `Keep ${project.leadReviewer}` : "Nobody yet"}
                </option>
                {reviewers.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.fullName} ({r.role.toLowerCase()})
                  </option>
                ))}
              </select>
            </label>
          </div>

          <label className="field">
            <span>Onboarding link <em>(optional)</em></span>
            <input name="onboardingUrl" type="url" value={form.onboardingUrl} onChange={update}
                   placeholder="https://github.com/your-org/repo/blob/main/CONTRIBUTING.md" />
            {fieldErrors.onboardingUrl && (
              <small className="field__error">{fieldErrors.onboardingUrl}</small>
            )}
          </label>

          <fieldset className="field audience">
            <span>Difficulty</span>
            {DIFFICULTIES.map((d) => (
              <label className="check" key={d.key}>
                <input type="radio" name="difficulty" value={d.key}
                       checked={form.difficulty === d.key} onChange={update} />
                <span>
                  {d.label}
                  <small className="field__hint"> — {d.hint}</small>
                </span>
              </label>
            ))}
          </fieldset>

          <div className="form__row">
            <label className="field">
              <span>Display order <em>(lower is first)</em></span>
              <input name="displayOrder" type="number" value={form.displayOrder} onChange={update} />
            </label>

            <div className="field">
              <span>Flags</span>
              <label className="check">
                <input type="checkbox" name="active" checked={form.active} onChange={update} />
                Open to new contributors
              </label>
              <small className="field__hint">
                Closing does not revoke anyone — existing access runs to its expiry.
              </small>
            </div>
          </div>

          {error && <p className="notice notice--error">{error}</p>}

          <div className="modal__actions">
            <button className="btn btn--primary" type="submit" disabled={busy}>
              {busy ? "Saving..." : project ? "Save project" : "Create project"}
            </button>
            <button className="btn btn--ghost" type="button" onClick={onCancel}>
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
