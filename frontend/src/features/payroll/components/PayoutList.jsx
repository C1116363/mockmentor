import { useState } from "react";
import ConfirmDialog from "../../../components/ConfirmDialog";
import { money, payoutTone } from "../payrollRules";

const fmt = (iso) =>
  iso ? new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" }) : "—";

/**
 * "Mark paid" needs the bank reference, so it is a dialog rather than a button.
 *
 * The reference is required by the server too. Its value shows up months later,
 * when a mentor says they were not paid and this is the thing that settles it -
 * so asking at the moment the money is sent, while the tab is still open, is
 * the only time it will actually get filled in.
 */
function MarkPaidDialog({ payout, onConfirm, onCancel }) {
  const [reference, setReference] = useState("");
  const [notes, setNotes] = useState("");

  return (
    <ConfirmDialog
      title={`Mark ₹${money(payout.amount)} to ${payout.mentorName} as paid`}
      intent="primary"
      confirmLabel="Record as paid"
      onCancel={onCancel}
      onConfirm={async () => {
        if (!reference.trim()) throw new Error("Enter the bank reference.");
        await onConfirm(reference.trim(), notes.trim() || null);
      }}
    >
      <p>
        Covers <strong>{payout.interviewCount}</strong> interview
        {payout.interviewCount === 1 ? "" : "s"} and{" "}
        <strong>{payout.mentoringCount}</strong> mentoring session
        {payout.mentoringCount === 1 ? "" : "s"}.
      </p>

      <label className="field">
        <span>Bank reference (UTR / NEFT)</span>
        <input value={reference} onChange={(e) => setReference(e.target.value)}
               placeholder="N123456789012345" autoFocus />
        <small className="field__hint">
          Required — it&apos;s what answers &quot;I never got paid&quot; six months from now.
        </small>
      </label>

      <label className="field">
        <span>Notes <em>(optional)</em></span>
        <input value={notes} onChange={(e) => setNotes(e.target.value)}
               placeholder="Paid with October's batch" />
      </label>
    </ConfirmDialog>
  );
}

/** Every payout, newest first. */
export default function PayoutList({ payouts, onMarkPaid, onCancel }) {
  const [paying, setPaying] = useState(null);
  const [cancelling, setCancelling] = useState(null);

  if (payouts.length === 0) {
    return (
      <p className="empty">
        No payouts yet. Raise one from a mentor&apos;s row once they have completed sessions.
      </p>
    );
  }

  return (
    <>
      <ul className="payout-list">
        {payouts.map((p) => (
          <li key={p.id} className={`payout payout--${payoutTone(p.status)}`}>
            <div className="payout__main">
              <div>
                <strong>{p.mentorName}</strong>
                <small className="payout__sub">
                  {p.interviewCount} interview{p.interviewCount === 1 ? "" : "s"} ·{" "}
                  {p.mentoringCount} mentoring · {fmt(p.periodStart)} – {fmt(p.periodEnd)}
                </small>
              </div>
              <div className="payout__amount">
                <strong>₹{money(p.amount)}</strong>
                <span className={`pill pill--${payoutTone(p.status)}`}>{p.status}</span>
              </div>
            </div>

            <div className="payout__meta">
              <span>Raised {fmt(p.createdAt)}{p.createdByName ? ` by ${p.createdByName}` : ""}</span>
              {p.status === "PAID" && (
                <span>
                  Paid {fmt(p.paidAt)}{p.paidByName ? ` by ${p.paidByName}` : ""} · ref{" "}
                  <code>{p.paymentReference}</code>
                </span>
              )}
              {p.status === "CANCELLED" && <span>Cancelled: {p.cancelledReason}</span>}
              {p.notes && <span className="payout__notes">{p.notes}</span>}
            </div>

            {p.status === "PENDING" && (
              <div className="payout__actions">
                <button className="btn btn--primary btn--sm" onClick={() => setPaying(p)}>
                  Mark paid
                </button>
                <button className="btn btn--ghost btn--sm" onClick={() => setCancelling(p)}>
                  Cancel
                </button>
              </div>
            )}
          </li>
        ))}
      </ul>

      {paying && (
        <MarkPaidDialog
          payout={paying}
          onCancel={() => setPaying(null)}
          onConfirm={async (reference, notes) => {
            await onMarkPaid(paying.id, reference, notes);
            setPaying(null);
          }}
        />
      )}

      {cancelling && (
        <ConfirmDialog
          title={`Cancel the ₹${money(cancelling.amount)} payout for ${cancelling.mentorName}?`}
          confirmLabel="Cancel this payout"
          cancelLabel="Keep it"
          reason={{
            label: "Why?",
            hint: "Kept on the payout, so the history explains itself later.",
            placeholder: "Raised against the wrong mentor",
            minLength: 4,
          }}
          onCancel={() => setCancelling(null)}
          onConfirm={async (reason) => {
            await onCancel(cancelling.id, reason);
            setCancelling(null);
          }}
        >
          <p>
            Its <strong>{cancelling.totalSessions}</strong> session
            {cancelling.totalSessions === 1 ? "" : "s"} go back to being owed, and will be
            picked up by the next payout for {cancelling.mentorName}.
          </p>
          <p className="muted-text">
            Only possible because it hasn&apos;t been paid. Once money has gone out, a payout
            can&apos;t be cancelled — that would put the same work back in line to be paid twice.
          </p>
        </ConfirmDialog>
      )}
    </>
  );
}
