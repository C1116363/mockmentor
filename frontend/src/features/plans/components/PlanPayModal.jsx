import { useEffect, useState } from "react";
import { usePlanPayment } from "../usePlanPayment";
import { formatPrice } from "../planRules";

/**
 * Pay for a plan. The same manual UPI flow as booking an interview, because it
 * is the same process: pay our UPI ID from your own app, send the UTR and a
 * screenshot, an admin confirms the money landed.
 *
 * The amount is fetched from the server, not passed in as a prop. It has to be:
 * the purchase froze the price when it was created, so if an admin has since
 * changed the plan, the number on this screen must be what the student actually
 * owes - not whatever the plan card happened to be showing.
 */
export default function PlanPayModal({ enrollment, onDone, onClose }) {
  const [upiReference, setUpiReference] = useState("");
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [copied, setCopied] = useState(false);

  // The amount comes from the purchase, which froze the price when it was made.
  const { instructions, error, setError, busy, submitProof } = usePlanPayment(enrollment.id);

  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  // Revoke the object URL so the preview doesn't leak.
  useEffect(() => () => preview && URL.revokeObjectURL(preview), [preview]);

  function pickFile(event) {
    const chosen = event.target.files?.[0];
    if (!chosen) return;
    setFile(chosen);
    setPreview((old) => {
      if (old) URL.revokeObjectURL(old);
      return URL.createObjectURL(chosen);
    });
  }

  async function copyUpi() {
    try {
      await navigator.clipboard.writeText(instructions.upiId);
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    } catch {
      setError("Couldn't copy — please select the ID and copy it manually.");
    }
  }

  async function submit(event) {
    event.preventDefault();
    try {
      await submitProof(upiReference.trim(), file);
      onDone();
    } catch {
      // usePlanPayment already put the message in `error` for us.
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-label="Pay for your plan">
        <header className="modal__head">
          <div>
            <h3>Pay for {enrollment.planName}</h3>
            <p className="modal__sub">Access starts as soon as an admin confirms the payment.</p>
          </div>
          <button className="modal__x" onClick={onClose} aria-label="Close">×</button>
        </header>

        {!instructions && !error && <p className="empty">Loading payment details...</p>}

        {instructions && (
          <>
            <div className="pay-amount">
              <span className="pay-amount__label">Amount</span>
              <span className="pay-amount__value">
                ₹{formatPrice(instructions.amount)}
              </span>
            </div>

            {/* The price was locked in when they clicked "Get this plan", so say
                so - otherwise a student who sees a different number on the
                pricing page thinks something is broken. */}
            <p className="pay-note">
              This is the price when you chose the plan. It stays fixed even if the
              listed price changes.
            </p>

            <ol className="pay-steps">
              <li>
                <strong>Pay this UPI ID</strong>
                <div className="upi-box">
                  <code>{instructions.upiId}</code>
                  <button type="button" className="btn btn--ghost btn--sm" onClick={copyUpi}>
                    {copied ? "Copied" : "Copy"}
                  </button>
                </div>
                <a className="upi-open" href={instructions.upiDeepLink}>
                  Open a UPI app (on your phone)
                </a>
                <small className="pay-note">
                  Paying to <strong>{instructions.payeeName}</strong>. On a laptop the link above
                  won&apos;t do anything — pay from your phone instead.
                </small>
              </li>

              <li>
                <strong>Send us the proof</strong>
                <form className="form" onSubmit={submit}>
                  <label className="field">
                    <span>UPI transaction / UTR number</span>
                    <input
                      value={upiReference}
                      onChange={(e) => setUpiReference(e.target.value)}
                      placeholder="e.g. 412345678901"
                      required
                    />
                  </label>

                  <label className="field">
                    <span>Screenshot from your UPI app</span>
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      onChange={pickFile}
                      required
                    />
                    <small className="field__hint">JPG, PNG or WebP, up to 5 MB.</small>
                  </label>

                  {preview && (
                    <img className="pay-preview" src={preview} alt="Payment screenshot preview" />
                  )}

                  <button
                    className="btn btn--primary btn--wide"
                    type="submit"
                    disabled={busy || !file || !upiReference.trim()}
                  >
                    {busy ? "Sending..." : "I've paid — submit for verification"}
                  </button>
                </form>
              </li>
            </ol>

            <p className="pay-note pay-note--foot">
              An admin usually confirms within a few hours. Your plan — and anything
              shared only with its members — unlocks the moment they do.
            </p>
          </>
        )}

        {error && <p className="notice notice--error">{error}</p>}
      </div>
    </div>
  );
}
