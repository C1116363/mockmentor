import { useEffect, useState } from "react";

const BLANK = {
  name: "",
  tagline: "",
  description: "",
  features: "",
  price: "",
  durationDays: 90,
  displayOrder: 0,
  highlighted: false,
  active: true,
};

/**
 * Create or edit a whole plan.
 *
 * Features are a textarea, one bullet per line, because that is how somebody
 * writing a pricing card actually thinks - and the backend stores them the same
 * way, splitting on newlines when it renders.
 */
export default function PlanEditor({ plan, onSave, onCancel }) {
  const [form, setForm] = useState(BLANK);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  // Editing an existing plan fills the form; "new" resets it. Keyed on id so
  // switching straight from one plan to another refills rather than keeping the
  // previous plan's text.
  useEffect(() => {
    if (!plan) {
      setForm(BLANK);
      return;
    }
    setForm({
      name: plan.name ?? "",
      tagline: plan.tagline ?? "",
      description: plan.description ?? "",
      // The API hands back a parsed array; the editor wants the raw lines.
      features: (plan.features ?? []).join("\n"),
      price: String(plan.price ?? ""),
      durationDays: plan.durationDays ?? 90,
      displayOrder: plan.displayOrder ?? 0,
      highlighted: plan.highlighted ?? false,
      active: plan.active ?? true,
    });
  }, [plan]);

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
        durationDays: Number(form.durationDays),
        displayOrder: Number(form.displayOrder),
      });
    } catch (err) {
      // fieldErrors comes from the same validation the API applies, so the
      // messages under the inputs are the server's, not a second set that could
      // disagree with it.
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
            <h3>{plan ? `Edit ${plan.name}` : "New plan"}</h3>
            <p className="modal__sub">
              {plan
                ? "Every field is replaced with what you send."
                : "It goes on sale immediately unless you untick “On sale”."}
            </p>
          </div>
          <button className="modal__x" onClick={onCancel} aria-label="Close">×</button>
        </header>

        <form className="form" onSubmit={submit}>
          <label className="field">
            <span>Name</span>
            <input name="name" value={form.name} onChange={update} required
                   placeholder="Placement Guide" />
            {fieldErrors.name && <small className="field__error">{fieldErrors.name}</small>}
          </label>

          <label className="field">
            <span>Tagline <em>(one line, shown under the name)</em></span>
            <input name="tagline" value={form.tagline} onChange={update}
                   placeholder="Everything from resume to offer letter" />
            {fieldErrors.tagline && <small className="field__error">{fieldErrors.tagline}</small>}
          </label>

          <div className="form__row">
            <label className="field">
              <span>Price (₹)</span>
              <input name="price" type="number" min="0" step="1" value={form.price}
                     onChange={update} required placeholder="2999" />
              {fieldErrors.price && <small className="field__error">{fieldErrors.price}</small>}
            </label>

            <label className="field">
              <span>Access (days)</span>
              <input name="durationDays" type="number" min="1" max="3650"
                     value={form.durationDays} onChange={update} required />
              {fieldErrors.durationDays && (
                <small className="field__error">{fieldErrors.durationDays}</small>
              )}
            </label>
          </div>

          <label className="field">
            <span>Description</span>
            <textarea name="description" rows={3} value={form.description} onChange={update}
                      placeholder="What the student actually gets, in a sentence or two." />
          </label>

          <label className="field">
            <span>Features <em>(one per line)</em></span>
            <textarea name="features" rows={5} value={form.features} onChange={update}
                      placeholder={"Resume review by a hiring manager\n4 mock interviews\nDSA sheet"} />
            <small className="field__hint">Each line becomes a bullet on the card.</small>
          </label>

          <label className="field field--narrow">
            <span>Display order <em>(lower is first)</em></span>
            <input name="displayOrder" type="number" value={form.displayOrder} onChange={update} />
          </label>

          <label className="toggle">
            <input type="checkbox" name="active" checked={form.active} onChange={update} />
            <span className="toggle__text">
              <strong>On sale</strong>
              <small>
                Taking it off sale hides it from students. Anyone who already bought
                it keeps their access.
              </small>
            </span>
          </label>

          <label className="toggle">
            <input type="checkbox" name="highlighted" checked={form.highlighted}
                   onChange={update} />
            <span className="toggle__text">
              <strong>Show the “Most popular” ribbon</strong>
              <small>Draws the eye to one plan. Only worth using on one of them.</small>
            </span>
          </label>

          {error && <p className="notice notice--error">{error}</p>}

          <div className="modal__actions">
            <button className="btn btn--primary" type="submit" disabled={busy}>
              {busy ? "Saving..." : plan ? "Save plan" : "Create plan"}
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
