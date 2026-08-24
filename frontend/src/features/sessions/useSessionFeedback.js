import { useCallback, useState } from "react";
import { sessionApi } from "../../api/sessionApi";

/** Closing one session: the mentor's scorecard, or notes for a mentoring session. */
export function useSessionFeedback(sessionId) {
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);

  const submit = useCallback(async (payload) => {
    setBusy(true);
    setError(null);
    setFieldErrors({});
    try {
      return await sessionApi.complete(sessionId, payload);
    } catch (e) {
      setError(e.message);
      setFieldErrors(e.fieldErrors ?? {});
      throw e;
    } finally {
      setBusy(false);
    }
  }, [sessionId]);

  return { error, fieldErrors, busy, submit };
}
