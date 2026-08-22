import { useEffect, useState } from "react";
import { fetchScreenshotUrl } from "../api/client";

/**
 * One payment for an admin to check against the bank statement.
 *
 * The screenshot needs the Authorization header, so it can't go straight into
 * an <img src>. We fetch it as a blob and use an object URL instead.
 */
export default function PaymentReviewCard({ payment, onVerify, onReject }) {
  const [imageUrl, setImageUrl] = useState(null);
  const [imageError, setImageError] = useState(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open || imageUrl || !payment.hasScreenshot) return;
    let revoked = false;
    fetchScreenshotUrl(payment.id)
      .then((url) => {
        if (revoked) URL.revokeObjectURL(url);
        else setImageUrl(url);
      })
      .catch(() => setImageError("Couldn't load the screenshot."));
    return () => {
      revoked = true;
    };
  }, [open, imageUrl, payment.id, payment.hasScreenshot]);

  // Release the object URL when the card goes away.
  useEffect(() => () => imageUrl && URL.revokeObjectURL(imageUrl), [imageUrl]);

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
