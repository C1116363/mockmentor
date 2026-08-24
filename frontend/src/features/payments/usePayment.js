import { useCallback, useEffect, useState } from "react";
import { paymentApi } from "../../api/paymentApi";

/**
 * Paying for one booked session: where to pay, and sending the proof.
 *
 * The instructions are fetched rather than passed in, because the amount comes
 * from server config and a client that could name its own price would be the
 * most obvious hole in a payment flow.
 */
export function usePayment(requestId) {
  const [instructions, setInstructions] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    paymentApi.instructions().then(setInstructions).catch((e) => setError(e.message));
  }, []);

  const submitProof = useCallback(async (upiReference, screenshot) => {
    setBusy(true);
    setError(null);
    try {
      return await paymentApi.submitProof(requestId, upiReference, screenshot);
    } catch (e) {
      setError(e.message);
      throw e;
    } finally {
      setBusy(false);
    }
  }, [requestId]);

  return { instructions, error, setError, busy, submitProof };
}
