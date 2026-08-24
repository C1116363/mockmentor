import { useState } from "react";
import MentorPayrollCard from "./MentorPayrollCard";
import PayoutList from "./PayoutList";
import { money, sortForPayroll } from "../payrollRules";

/**
 * The payroll screen.
 *
 * Two views rather than one long page: "who needs paying" and "what has been
 * paid" are different jobs done at different moments, and stacking them means
 * the history pushes the thing you came to do off the bottom of the screen.
 */
export default function PayrollSection({ payroll }) {
  const [view, setView] = useState("mentors");
  const { mentors, summary, payouts, loading, error, message, setMessage, setError } = payroll;

  if (loading) return <p className="empty">Loading payroll...</p>;

  const sorted = sortForPayroll(mentors);
  const owed = sorted.filter((m) => m.unpaidInterviews + m.unpaidMentoring > 0);
  const rest = sorted.filter((m) => m.unpaidInterviews + m.unpaidMentoring === 0);
  const pendingCount = payouts.filter((p) => p.status === "PENDING").length;

  return (
    <section className="payroll">
      {summary && (
        <header className="payroll__summary">
          <div>
            <span className="payroll__summary-label">Owed right now</span>
            <strong className="payroll__summary-value">₹{money(summary.totalOwed)}</strong>
            <small>
              across {summary.mentorsWithWorkOwed} mentor
              {summary.mentorsWithWorkOwed === 1 ? "" : "s"}
            </small>
          </div>
          <div>
            <span className="payroll__summary-label">Raised, not yet sent</span>
            <strong className="payroll__summary-value">₹{money(summary.pendingAmount)}</strong>
            <small>{summary.pendingPayouts} payout{summary.pendingPayouts === 1 ? "" : "s"}</small>
          </div>
          <div>
            <span className="payroll__summary-label">Paid to date</span>
            <strong className="payroll__summary-value">₹{money(summary.totalPaid)}</strong>
            <small>{summary.mentorsOnPayroll} on payroll</small>
          </div>
        </header>
      )}

      <div className="tabs">
        <button className={`tab ${view === "mentors" ? "tab--on" : ""}`}
                onClick={() => setView("mentors")}>
          Mentors{owed.length > 0 ? ` (${owed.length} owed)` : ""}
        </button>
        <button className={`tab ${view === "payouts" ? "tab--on" : ""}`}
                onClick={() => setView("payouts")}>
          Payouts{pendingCount > 0 ? ` (${pendingCount} pending)` : ""}
        </button>
      </div>

      {message && (
        <p className="notice notice--success" onAnimationEnd={() => setMessage(null)}>{message}</p>
      )}
      {error && (
        <p className="notice notice--error" onClick={() => setError(null)}>{error}</p>
      )}

      {view === "mentors" ? (
        <>
          {owed.length === 0 && (
            <p className="empty">Nobody is owed anything right now.</p>
          )}

          {owed.map((m) => (
            <MentorPayrollCard key={m.mentorId} mentor={m}
                               onConfigure={payroll.configure}
                               onCreatePayout={payroll.createPayout} />
          ))}

          {/* Kept on the page rather than filtered away - a mentor with nothing
              outstanding is exactly who you look for when you want to check or
              change their rate. */}
          {rest.length > 0 && (
            <details className="payroll__rest">
              <summary>{rest.length} mentor{rest.length === 1 ? "" : "s"} with nothing outstanding</summary>
              {rest.map((m) => (
                <MentorPayrollCard key={m.mentorId} mentor={m}
                                   onConfigure={payroll.configure}
                                   onCreatePayout={payroll.createPayout} />
              ))}
            </details>
          )}
        </>
      ) : (
        <PayoutList payouts={payouts}
                    onMarkPaid={payroll.markPaid}
                    onCancel={payroll.cancelPayout} />
      )}
    </section>
  );
}
