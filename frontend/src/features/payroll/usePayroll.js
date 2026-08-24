import { useCallback, useEffect, useState } from "react";
import { payrollApi } from "../../api/payrollApi";

/**
 * The admin payroll screen: mentors, what they are owed, and payouts.
 *
 * <h2>Everything reloads after every write</h2>
 * Deliberately, rather than patching the row in place. Raising a payout changes
 * that mentor's owed figure to zero, the summary totals, and the payout history
 * all at once - and cancelling one changes them all back. Reconciling that by
 * hand in three places is how a screen ends up telling an admin a mentor is
 * owed money that has already been paid, which is worse than a spinner.
 */
export function usePayroll() {
  const [mentors, setMentors] = useState([]);
  const [summary, setSummary] = useState(null);
  const [payouts, setPayouts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  const reload = useCallback(async () => {
    setError(null);
    try {
      const [m, s, p] = await Promise.all([
        payrollApi.mentors(),
        payrollApi.summary(),
        payrollApi.payouts(),
      ]);
      setMentors(m);
      setSummary(s);
      setPayouts(p);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  /**
   * Run a write, show what the server said, then reload.
   *
   * The server's own message is used rather than a local one, because for
   * payroll it carries the numbers - "2 interviews + 2 mentoring = ₹2,600" -
   * and those are exactly what an admin needs to see at the moment they act.
   */
  const run = useCallback(async (action) => {
    setError(null);
    setMessage(null);
    try {
      const result = await action();
      if (result?.message) setMessage(result.message);
      await reload();
      return result;
    } catch (e) {
      setError(e.message);
      throw e;
    }
  }, [reload]);

  return {
    mentors,
    summary,
    payouts,
    loading,
    error,
    setError,
    message,
    setMessage,

    configure: (mentorId, settings) => run(() => payrollApi.configure(mentorId, settings)),
    createPayout: (mentorId) => run(() => payrollApi.createPayout(mentorId)),
    markPaid: (payoutId, reference, notes) =>
      run(() => payrollApi.markPaid(payoutId, reference, notes)),
    cancelPayout: (payoutId, reason) => run(() => payrollApi.cancelPayout(payoutId, reason)),
    reload,
  };
}
