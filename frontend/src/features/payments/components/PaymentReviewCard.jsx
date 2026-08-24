import { useState } from "react";
import { usePaymentScreenshot } from "../usePaymentScreenshot";

/**
 * One payment for an admin to check against the bank statement.
 *
 * The screenshot needs the Authorization header, so it can't go straight into an
 * <img src>. useBlobUrl fetches it and manages the object URL's lifetime.
 */
export default function PaymentReviewCard({ payment, onVerify, onReject }) {
  const [open, setOpen] = useState(false);

  // Fetched only once the card is expanded, so a queue of thirty does not pull
  // thirty screenshots nobody asked to see. The hook revokes the object URL
  // when this card unmounts, and ignores a response that lands after that.
  const { url: imageUrl, error: imageError } = usePaymentScreenshot(payment, open);

  return (
    <article className="card">
      <header className="card__head">
        <div>
          <h4 className="card__title">₹{payment.amount}</h4>
          <p className="card__sub">
            {payment.studentName} · {payment.topic}
          </p>
        </div>
        <span className="badge badge--pending">TO CHECK</span>
      </header>

      <dl className="card__facts">
        <div>
          <dt>UTR / reference</dt>
          <dd className="mono">{payment.upiReference ?? "—"}</dd>
        </div>
        <div>
          <dt>Sent</dt>
          <dd>{payment.submittedAt ? new Date(payment.submittedAt).toLocaleString() : "—"}</dd>
        </div>
        <div>
          <dt>Slot booked</dt>
          <dd>{payment.slot ? new Date(payment.slot).toLocaleString() : "—"}</dd>
        </div>
        <div>
          <dt>Request</dt>
          <dd>#{payment.requestId}</dd>
        </div>
      </dl>

      {payment.hasScreenshot ? (
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

      <p className="pay-note">
        Check this UTR against your bank before verifying. Verifying releases the
        booking to mentors.
      </p>

      <div className="accept-form__actions">
        <button className="btn btn--primary" onClick={() => onVerify(payment)}>
          Payment received
        </button>
        <button className="btn btn--ghost" onClick={() => onReject(payment)}>
          Reject
        </button>
      </div>
    </article>
  );
}
