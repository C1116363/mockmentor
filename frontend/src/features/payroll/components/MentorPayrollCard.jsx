import { useState } from "react";
import RateEditor from "./RateEditor";
import { blockedReason, canCreatePayout, missingBankDetails, money } from "../payrollRules";

/**
 * One mentor's payroll row.
 *
 * <h2>Bank details are behind a click</h2>
 * An account number is not something to leave on screen while somebody screen
 * shares or walks past a desk - and the row is scanned far more often than the
 * details are needed. Shown in full when asked for, since the point is to
 * transfer money without leaving the page.
 */
export default function MentorPayrollCard({ mentor, onConfigure, onCreatePayout }) {
  const [editing, setEditing] = useState(false);
  const [showBank, setShowBank] = useState(false);
  const [busy, setBusy] = useState(false);

  const owedSessions = mentor.unpaidInterviews + mentor.unpaidMentoring;
  const blocked = blockedReason(mentor);
  const payable = canCreatePayout(mentor);

  async function raise() {
    setBusy(true);
    try {
      await onCreatePayout(mentor.mentorId);
    } catch {
      // usePayroll surfaces the message.
    } finally {
      setBusy(false);
    }
  }

  return (
    <article className={`payroll-card ${owedSessions > 0 ? "payroll-card--owed" : ""}`}>
      <header className="payroll-card__head">
        <div className="payroll-card__who">
          <strong>{mentor.mentorName}</strong>
          <small>{mentor.mentorEmail}</small>
        </div>

        <div className="payroll-card__flags">
          {mentor.payrollEnabled ? (
            <span className="pill pill--success">On payroll</span>
          ) : (
            <span className="pill pill--muted">Payroll off</span>
          )}
          {mentor.hasPendingPayout && <span className="pill pill--warning">Payout pending</span>}
          {mentor.verificationStatus !== "APPROVED" && (
            <span className="pill pill--muted">{mentor.verificationStatus}</span>
          )}
        </div>
      </header>

      <div className="payroll-card__figures">
        <div className="figure">
          <span className="figure__value">{mentor.unpaidInterviews}</span>
          <span className="figure__label">interviews</span>
        </div>
        <span className="figure__plus">+</span>
        <div className="figure">
          <span className="figure__value">{mentor.unpaidMentoring}</span>
          <span className="figure__label">mentoring</span>
        </div>

        <div className="payroll-card__due">
          <span className="figure__label">Owed now</span>
          <strong className="payroll-card__amount">₹{money(mentor.amountDue)}</strong>
          {/* The counts are real work even when there is no rate to price it
              with, so say so rather than showing a confident ₹0. */}
          {owedSessions > 0 && Number(mentor.amountDue) === 0 && (
            <small className="payroll-card__unpriced">no rate set yet</small>
          )}
        </div>
      </div>

      <div className="payroll-card__rates">
        {mentor.interviewRate == null ? (
          <span className="muted-text">No rates set</span>
        ) : (
          <span className="muted-text">
            ₹{money(mentor.interviewRate)} / interview · ₹{money(mentor.mentoringRate)} / mentoring
          </span>
        )}
        <button type="button" className="linkish" onClick={() => setEditing((v) => !v)}>
          {editing ? "Close" : mentor.interviewRate == null ? "Set rates" : "Edit rates"}
        </button>
      </div>

      {editing && (
        <RateEditor
          mentor={mentor}
          onCancel={() => setEditing(false)}
          onSave={async (settings) => {
            await onConfigure(mentor.mentorId, settings);
            setEditing(false);
          }}
        />
      )}

      {missingBankDetails(mentor) && (
        <p className="notice notice--warning payroll-card__warn">
          No bank details on file — you can still raise the payout, but you won&apos;t be
          able to send the money until they complete their profile.
        </p>
      )}

      <footer className="payroll-card__foot">
        <div className="payroll-card__meta">
          <span>Paid to date: <strong>₹{money(mentor.totalPaid)}</strong></span>
          {mentor.bankDetailsComplete && (
            <button type="button" className="linkish" onClick={() => setShowBank((v) => !v)}>
              {showBank ? "Hide bank details" : "Bank details"}
            </button>
          )}
        </div>

        <button
          className="btn btn--primary btn--sm"
          type="button"
          onClick={raise}
          disabled={!payable || busy}
          title={blocked ?? "Raise a payout for everything owed"}
        >
          {busy ? "Working..." : `Raise payout ₹${money(mentor.amountDue)}`}
        </button>
      </footer>

      {blocked && owedSessions > 0 && <small className="payroll-card__blocked">{blocked}</small>}

      {showBank && (
        <dl className="bank-details">
          <div><dt>Account holder</dt><dd>{mentor.bankAccountHolder}</dd></div>
          <div><dt>Account number</dt><dd>{mentor.bankAccountNumber}</dd></div>
          <div><dt>IFSC</dt><dd>{mentor.bankIfsc}</dd></div>
          <div><dt>Bank</dt><dd>{mentor.bankName}</dd></div>
          {mentor.panNumber && <div><dt>PAN</dt><dd>{mentor.panNumber}</dd></div>}
        </dl>
      )}
    </article>
  );
}
