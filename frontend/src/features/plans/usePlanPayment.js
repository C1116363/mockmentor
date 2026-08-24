import { useCallback, useEffect, useState } from "react";
import { planApi } from "../../api/planApi";

/**
 * Paying for one plan purchase.
 *
 * The amount comes from the purchase, which froze the price when it was created -
 * so if an admin has since changed the plan, this still shows what the student
 * actually owes rather than today's number.
 */
export function usePlanPayment(enrollmentId) {
  const [instructions, setInstructions] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    planApi
      .paymentInstructions(enrollmentId)
      .then(setInstructions)
      .catch((e) => setError(e.message));
  }, [enrollmentId]);

  const submitProof = useCallback(async (upiReference, screenshot) => {
    setBusy(true);
    setError(null);
    try {
      return await planApi.submitProof(enrollmentId, upiReference, screenshot);
    } catch (e) {
      setError(e.message);
      throw e;
    } finally {
      setBusy(false);
    }
  }, [enrollmentId]);

  return { instructions, error, setError, busy, submitProof };
}
