import { useCallback, useEffect, useState } from "react";
import { sessionApi } from "../../api/sessionApi";
import { partitionSessions, selectUpcoming } from "./sessionRules";

/**
 * A student's bookings: the data, the derived lists, and the actions.
 *
 * Backend counterpart: the facade layer. It composes the api module with the
 * pure rules and hands the screen a finished answer, so the page renders and
 * delegates and holds no fetch logic of its own.
 *
 * Each feature gets its own hook rather than one hook for the whole dashboard.
 * That is what stops a failing plans call from blanking the interview list -
 * they load and fail independently.
 */
export function useSessions() {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const reload = useCallback(async () => {
    setSessions(await sessionApi.mine());
  }, []);

  useEffect(() => {
    reload()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [reload]);

  const book = useCallback(async (form) => {
    const created = await sessionApi.create(form);
    await reload();
    return created;
  }, [reload]);

  const cancel = useCallback(async (id) => {
    await sessionApi.cancel(id);
    await reload();
  }, [reload]);

  const { live, done, unpaidCount } = partitionSessions(sessions);

  return {
    sessions, live, done, unpaidCount,
    upcoming: selectUpcoming(sessions),
    loading, error,
    reload, book, cancel,
  };
}
