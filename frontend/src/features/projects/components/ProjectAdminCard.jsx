import { useEffect, useState } from "react";
import { formatPrice } from "../../../utils/format";

/**
 * One live project in the admin list, with the price editable in place.
 *
 * The repository is shown as owner/name with a link, because that is the thing an
 * admin needs to check against reality - a project pointing at a repo that does
 * not exist looks completely fine here until somebody pays and the invite 404s.
 */
export default function ProjectAdminCard({ project: plan, onSavePrice, onToggleActive,
                                          onEdit, onViewContributors }) {
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
          <p className="card__sub">
            {plan.summary ?? "—"}
            {plan.repoFullName && (
              <>
                <span className="card__sub-sep">·</span>
                <a className="mono" href={plan.repoUrl} target="_blank" rel="noopener noreferrer">
                  {plan.repoFullName}
                </a>
              </>
            )}
          </p>
        </div>
        <span className={`badge ${plan.active ? "badge--completed" : "badge--cancelled"}`}>
          {plan.active ? "OPEN" : "CLOSED"}
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
          <dd>{plan.accessDurationDays} days</dd>
        </div>
        <div>
          <dt>Contributors</dt>
          <dd>
            {plan.seatsTaken}
            {plan.maxContributors ? ` / ${plan.maxContributors}` : " (no limit)"}
            {!plan.seatsAvailable && <span className="badge badge--cancelled"> FULL</span>}
          </dd>
        </div>
        <div>
          <dt>Reviewer</dt>
          <dd>{plan.leadReviewer ?? "—"}</dd>
        </div>
        <div>
          <dt>Last changed</dt>
          <dd>{plan.updatedAt ? new Date(plan.updatedAt).toLocaleString() : "—"}</dd>
        </div>
      </dl>

      {error && <p className="notice notice--error">{error}</p>}

      {editing && (
        <p className="pay-note">
          Live for the next request. Anyone who already requested access keeps the
          price they were quoted.
        </p>
      )}

      <div className="accept-form__actions">
        <button className="btn btn--ghost btn--sm" onClick={() => onEdit(plan)}>
          Edit details
        </button>
        <button className="btn btn--ghost btn--sm" onClick={() => onViewContributors(plan)}>
          Contributors ({plan.seatsTaken})
        </button>
        <button className="btn btn--ghost btn--sm" onClick={() => onToggleActive(plan)}>
          {plan.active ? "Close to new" : "Open to new"}
        </button>
      </div>
    </article>
  );
}
