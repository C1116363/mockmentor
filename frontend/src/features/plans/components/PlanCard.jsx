import { formatPrice } from "../../../utils/format";
/**
 * One plan on the pricing grid.
 *
 * The price shown here is whatever the admin last set - it comes straight from
 * the API on every load, never from anything cached. A stale number on a buy
 * button is the one thing this feature must not do.
 */
export default function PlanCard({ plan, enrollment, onGet, onPay, busy }) {
  // What the student holds right now decides what the button says. Reading it
  // from the enrollment rather than tracking separate state means the card
  // cannot disagree with the server about whether they own this.
  const active = enrollment?.currentlyActive;
  const awaiting = enrollment?.status === "AWAITING_PAYMENT";
  const checking = enrollment?.status === "SUBMITTED";
  const rejected = enrollment?.status === "REJECTED";

  return (
    <article className={`plan ${plan.highlighted ? "plan--featured" : ""} ${active ? "plan--owned" : ""}`}>
      {plan.highlighted && <span className="plan__ribbon">Most popular</span>}

      <header className="plan__head">
        <h3 className="plan__name">{plan.name}</h3>
        {plan.tagline && <p className="plan__tagline">{plan.tagline}</p>}
      </header>

      <p className="plan__price">
        <span className="plan__rupee">₹</span>
        {/* toLocaleString so 2999 reads as 2,999 - Indian grouping */}
        <span className="plan__amount">{formatPrice(plan.price)}</span>
      </p>
      <p className="plan__duration">
        {plan.durationDays} days of access
      </p>

      {plan.description && <p className="plan__desc">{plan.description}</p>}

      {plan.features.length > 0 && (
        <ul className="plan__features">
          {plan.features.map((feature) => (
            <li key={feature}>{feature}</li>
          ))}
        </ul>
      )}

      <div className="plan__foot">
        {active && (
          <>
            <span className="badge badge--completed">Active</span>
            {enrollment.expiresAt && (
              <small className="plan__note">
                Until {new Date(enrollment.expiresAt).toLocaleDateString()}
              </small>
            )}
          </>
        )}

        {checking && (
          <>
            <span className="badge badge--pending">Checking payment</span>
            <small className="plan__note">
              An admin is confirming your UPI reference. This usually takes a few hours.
            </small>
          </>
        )}

        {awaiting && (
          <button className="btn btn--primary btn--wide" onClick={() => onPay(enrollment)}>
            Finish payment
          </button>
        )}

        {rejected && (
          <>
            <span className="badge badge--rejected">Payment rejected</span>
            {enrollment.rejectionReason && (
              <small className="plan__note">{enrollment.rejectionReason}</small>
            )}
            <button className="btn btn--primary btn--wide" onClick={() => onPay(enrollment)}>
              Send new proof
            </button>
          </>
        )}

        {!enrollment && (
          <button
            className="btn btn--primary btn--wide"
            onClick={() => onGet(plan)}
            disabled={busy}
          >
            {busy ? "Starting..." : "Get this plan"}
          </button>
        )}
      </div>
    </article>
  );
}
