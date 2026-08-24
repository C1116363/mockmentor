import { useCallback, useEffect, useState } from "react";
import { projectApi } from "../../api/projectApi";

/**
 * Paying for one project access request.
 *
 * The amount comes from the request, which froze the price when it was created -
 * so if an admin has since changed the project, this still shows what the student
 * actually owes.
 */
export function useProjectAccessPayment(accessId) {
  const [instructions, setInstructions] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    projectApi
      .paymentInstructions(accessId)
      .then(setInstructions)
      .catch((e) => setError(e.message));
  }, [accessId]);

  const submitProof = useCallback(async (upiReference, screenshot) => {
    setBusy(true);
    setError(null);
    try {
      return await projectApi.submitProof(accessId, upiReference, screenshot);
    } catch (e) {
      setError(e.message);
      throw e;
    } finally {
      setBusy(false);
    }
  }, [accessId]);

  return { instructions, error, setError, busy, submitProof };
}
