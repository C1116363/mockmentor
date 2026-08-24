import { useState } from "react";
import { useEnrollmentScreenshot } from "../useEnrollmentScreenshot";
import { formatPrice } from "../../../utils/format";

/**
 * One plan payment for an admin to check against the bank statement.
 *
 * Same job as PaymentReviewCard does for interviews, against the plan endpoints.
 * Kept separate rather than parameterised: the two flows have different fields
 * worth showing (a slot and topic versus a plan and its access window), and
 * folding them together would mean a card full of conditionals to save a dozen
 * lines.
 */
export default function EnrollmentReviewCard({ enrollment, onActivate, onReject }) {
  const [open, setOpen] = useState(false);

  // Fetched only once the card is expanded, so a queue of thirty does not pull
  // thirty screenshots nobody asked to see. The hook revokes the object URL
  // when this card unmounts, and ignores a response that lands after that.
  const { url: imageUrl, error: imageError } = useEnrollmentScreenshot(enrollment, open);

  return (
    <article className="card">
      <header className="card__head">
        <div>
          <h4 className="card__title">
            ₹{formatPrice(enrollment.pricePaid)}
          </h4>
          <p className="card__sub">
            {enrollment.studentName} · {enrollment.planName}
          </p>
        </div>
        <span className="badge badge--pending">TO CHECK</span>
      </header>

      <dl className="card__facts">
        <div>
          <dt>UTR / reference</dt>
          <dd className="mono">{enrollment.upiReference ?? "—"}</dd>
        </div>
        <div>
          <dt>Sent</dt>
          <dd>
            {enrollment.submittedAt
              ? new Date(enrollment.submittedAt).toLocaleString()
              : "—"}
          </dd>
        </div>
        <div>
          <dt>Student</dt>
          <dd className="muted">{enrollment.studentEmail}</dd>
        </div>
        <div>
          <dt>Purchase</dt>
          <dd>#{enrollment.id}</dd>
        </div>
      </dl>

      {enrollment.hasScreenshot ? (
        <>
          <button className="linkish" onClick={() => setOpen((v) => !v)}>
            {open ? "Hide screenshot" : "Show screenshot"}
          </button>
          {open && (
            <>
              {imageError && <p className="notice notice--error">{imageError}</p>}
              {!imageUrl && !imageError && <p className="empty">Loading image...</p>}
              {imageUrl && <img className="pay-shot" src={imageUrl} alt="Payment screenshot" />}
            </>
          )}
        </>
      ) : (
        <p className="empty">No screenshot uploaded.</p>
      )}

      {/* The amount shown is what this student was charged when they chose the
          plan, which may not be the plan's current price. Say so, or an admin
          comparing it against the pricing page thinks the bank is short. */}
      <p className="pay-note">
        Check this UTR against your bank before confirming. The amount is the price
        when they bought, not necessarily today&apos;s. Confirming starts their access.
      </p>

      <div className="accept-form__actions">
        <button className="btn btn--primary" onClick={() => onActivate(enrollment)}>
          Payment received
        </button>
        <button className="btn btn--ghost" onClick={() => onReject(enrollment)}>
          Reject
        </button>
      </div>
    </article>
  );
}
