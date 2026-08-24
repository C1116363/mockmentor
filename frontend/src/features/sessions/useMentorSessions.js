import { useCallback, useEffect, useState } from "react";
import { sessionApi } from "../../api/sessionApi";

/**
 * The mentor's side of bookings: the open queue and their own accepted ones.
 *
 * Its own file rather than a second export from useSessions.js - a student hook
 * and a mentor hook have no shared state, and one file exporting both invites
 * importing the wrong one.
 */
export function useMentorSessions() {
  const [queue, setQueue] = useState([]);
  const [assigned, setAssigned] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const reload = useCallback(async () => {
    // Both together: a mentor accepting from the queue changes both lists, and
    // refreshing only one leaves the screen contradicting itself.
    const [q, a] = await Promise.all([sessionApi.openQueue(), sessionApi.assignedToMe()]);
    setQueue(q);
    setAssigned(a);
  }, []);

  useEffect(() => {
    reload()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [reload]);

  const accept = useCallback(async (id, payload) => {
    await sessionApi.accept(id, payload);
    await reload();
  }, [reload]);

  const complete = useCallback(async (id, payload) => {
    await sessionApi.complete(id, payload);
    await reload();
  }, [reload]);

  return { queue, assigned, loading, error, reload, accept, complete };
}
