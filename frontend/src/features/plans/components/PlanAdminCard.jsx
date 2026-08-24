import { useEffect, useState } from "react";
import { formatPrice } from "../planRules";

/**
 * One plan in the admin list, with the price editable in place.
 *
 * Editing the price is its own small form hitting its own endpoint, rather than
 * part of a full plan edit. Two reasons: it is by far the most common change, and
 * a full-plan PUT from a partly-filled form is how a description gets silently
 * blanked.
 */
export default function PlanAdminCard({ plan, onSavePrice, onToggleActive, onEdit }) {
  const [editing, setEditing] = useState(false);
  const [price, setPrice] = useState(String(plan.price));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  // If the plan changes underneath us (another admin, or our own reload), take
  // the new price - otherwise the input keeps showing a number that is no
  // longer true.
  useEffect(() => {
    if (!editing) setPrice(String(plan.price));
  }, [plan.price, editing]);

  async function save(event) {
    event.preventDefault();
    setError(null);

    const value = Number(price);
    // Checked here as well as on the server. Not instead of - this is only so
    // the admin gets the answer without a round trip.
    if (!Number.isFinite(value) || value < 0) {
      setError("Enter a price of 0 or more.");
      return;
    }

    setBusy(true);
    try {
      await onSavePrice(plan, value);
      setEditing(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <article className={`card ${plan.active ? "" : "card--muted"}`}>
      <header className="card__head">
        <div>
          <h4 className="card__title">{plan.name}</h4>
          <p className="card__sub">{plan.tagline ?? "—"}</p>
        </div>
        <span className={`badge ${plan.active ? "badge--completed" : "badge--cancelled"}`}>
          {plan.active ? "ON SALE" : "RETIRED"}
        </span>
      </header>

      <dl className="card__facts">
        <div>
          <dt>Price</dt>
          <dd className="price-cell">
            {editing ? (
              <form className="price-edit" onSubmit={save}>
                <span aria-hidden="true">₹</span>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                  aria-label={`New price for ${plan.name}`}
                  autoFocus
                />
                <button className="btn btn--primary btn--sm" type="submit" disabled={busy}>
                  {busy ? "..." : "Save"}
                </button>
                <button
                  className="btn btn--ghost btn--sm"
                  type="button"
                  onClick={() => {
                    setEditing(false);
                    setPrice(String(plan.price));
                    setError(null);
                  }}
                >
                  Cancel
                </button>
              </form>
            ) : (
              <>
                <strong>₹{formatPrice(plan.price)}</strong>
                <button className="linkish" onClick={() => setEditing(true)}>
                  Change price
                </button>
              </>
            )}
          </dd>
        </div>
        <div>
          <dt>Access</dt>
          <dd>{plan.durationDays} days</dd>
        </div>
        <div>
          <dt>Order</dt>
          <dd>{plan.displayOrder}</dd>
        </div>
        <div>
          <dt>Last changed</dt>
          <dd>{plan.updatedAt ? new Date(plan.updatedAt).toLocaleString() : "—"}</dd>
        </div>
      </dl>

      {error && <p className="notice notice--error">{error}</p>}

      {editing && (
        <p className="pay-note">
          Students see the new price on their next page load. Anyone who already
          bought this plan keeps the price they paid.
        </p>
      )}

      <div className="accept-form__actions">
        <button className="btn btn--ghost btn--sm" onClick={() => onEdit(plan)}>
          Edit details
        </button>
        <button className="btn btn--ghost btn--sm" onClick={() => onToggleActive(plan)}>
          {plan.active ? "Take off sale" : "Put on sale"}
        </button>
      </div>
    </article>
  );
}
