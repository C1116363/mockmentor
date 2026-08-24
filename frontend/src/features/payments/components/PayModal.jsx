import { useEffect, useRef, useState } from "react";
import { usePayment } from "../usePayment";
import GatewayPayPanel from "../../checkout/components/GatewayPayPanel";

/**
 * "Pay and book" - shown right after a slot is reserved.
 *
 * There is no payment gateway in this version. The student pays our UPI ID from
 * their own app, then sends the UTR and a screenshot; an admin confirms the
 * money landed. Until they do, the booking sits in AWAITING_PAYMENT and no
 * mentor can see it.
 */
export default function PayModal({ request, onDone, onClose }) {
  const [upiReference, setUpiReference] = useState("");
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [copied, setCopied] = useState(false);
  const dialogRef = useRef(null);

  // The payment feature's hook: where to pay, and sending the proof.
  const { instructions, error, setError, busy, submitProof } = usePayment(request.id);

  // Close on Escape, like any dialog should.
  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  // Revoke the object URL so we don't leak it when the preview changes.
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
      // usePayment already put the message in `error` for us.
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" ref={dialogRef} role="dialog" aria-modal="true" aria-label="Pay to confirm booking">
        <header className="modal__head">
          <div>
            <h3>Pay to confirm your slot</h3>
            <p className="modal__sub">{request.topic}</p>
          </div>
          <button className="modal__x" onClick={onClose} aria-label="Close">×</button>
        </header>

        {!instructions && !error && <p className="empty">Loading payment details...</p>}

        {instructions && (
          <>
            <div className="pay-amount">
              <span className="pay-amount__label">Amount</span>
              <span className="pay-amount__value">₹{instructions.amount}</span>
            </div>

            {/* Renders nothing unless a gateway is configured and its keys are
                present, so this screen is unchanged on a manual-UPI server.
                INTERVIEW is addressed by request id - see PaymentServiceImpl. */}
            <GatewayPayPanel
              purpose="INTERVIEW"
              targetId={request.id}
              amount={instructions.amount}
              onPaid={onDone}
            />

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

                  {preview && <img className="pay-preview" src={preview} alt="Payment screenshot preview" />}

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
              Your slot is held while we check. An admin usually confirms within a few hours, and
              your interview is only released to mentors once payment is verified.
            </p>
          </>
        )}

        {error && <p className="notice notice--error">{error}</p>}
      </div>
    </div>
  );
}
